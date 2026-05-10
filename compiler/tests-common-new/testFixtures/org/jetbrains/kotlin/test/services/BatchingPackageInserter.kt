/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.mock.MockProject
import com.intellij.pom.PomModel
import com.intellij.pom.core.impl.PomModelImpl
import com.intellij.pom.tree.TreeAspect
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.TreeCopyHandler
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.cli.create
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.psi.psiUtil.nextLeaf
import org.jetbrains.kotlin.psi.psiUtil.prevLeaf
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.impl.shouldIsolateTestInGroupingConfiguration
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.util.capitalizeDecapitalize.decapitalizeSmart
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

/**
 * For every Kotlin file (*.kt) stored in this text:
 *
 * - If there is a "package" declaration, patch it to prepend a unique package prefix.
 *   Example: package foo -> package codegen.box.annotations.genericAnnotations.foo
 *
 * - If there is no "package" declaration, add one with the package name equal to the unique package prefix.
 *   Example (new line added): package codegen.box.annotations.genericAnnotations
 *
 * - All "import" declarations are patched to reflect appropriate changes in "package" declarations.
 *   Example: import foo.* -> import codegen.box.annotations.genericAnnotations.foo.*
 *
 * - All fully qualified references are patched to reflect appropriate changes in "package" declarations.
 *   Example: val x = foo.Bar() -> val x = codegen.box.annotations.genericAnnotations.foo.Bar()
 *
 * The "unique package prefix" is computed individually for every test file and reflects a relative path to the test file.
 * Example: codegen/box/annotations/genericAnnotations.kt -> codegen.box.annotations.genericAnnotations
 *
 * Note that packages with fully qualified names starting with "kotlin." and "helpers." are kept unchanged.
 * Examples: package kotlin.coroutines -> package kotlin.coroutines
 *           import kotlin.test.* -> import kotlin.test.*
 */
class BatchingPackageInserter(testServices: TestServices) : ReversibleSourceFilePreprocessor(testServices) {
    companion object {
        private val lock = Any()

        fun computePackage(testInfo: KotlinTestInfo): String {
            val (className, methodName, _) = testInfo
            val classPart = className.substringAfter("$")
                .split("$")
                .map { it.decapitalizeSmart() }
                .joinToString(".") {
                    // In the original implementation of this logic in the Native test infra we wrapped with '_' only these
                    // names which were clashing with hard keywords in the language. But for some reason using the same
                    // approach here leads to a strange situation, when `PackageNamePatcher.visitKtElement` at each
                    // invocation creates new instances of `KtDotQualifiedExpression` on stack, which leads to several
                    // GBs (!) of PSI being allocated on stack which causes the OOM when running a lot of tests at once.
                    // And adding the `_` unconditionally for some reason fixes this problem (TBH, I have no idea why).
                    "_${it}_"
                }
            return "$classPart.$methodName"
        }
    }

    private val packageMapping: MutableMap<FqName, FqName> = mutableMapOf()

    override fun process(file: TestFile, content: String): String {
        shouldNotBeCalled()
    }

    @TestInfrastructureInternals
    override fun processModule(module: TestModule, filesContent: MutableMap<TestFile, String>) {
        if (testServices.shouldIsolateTestInGroupingConfiguration(fileGenerationPhase = true))
            return // Without grouping, packages are not altered, since no clashes can happen.

        // At this point we can't get `project` from `compilerConfigurationProvider`, as it will cause infinite recursion.
        val psiFactory = createPsiFactory()
        val additionalBasePackage = FqName(computePackage(testServices.testInfo))
        val ktFiles = filesContent.filter { it.key.isKtFile }
            .mapValues { (file, content) -> psiFactory.createFile(file.name, content) }
        ktFiles.values.map { it.packageFqName }.associateWithTo(packageMapping) { packageFqName ->
            additionalBasePackage.child(packageFqName)
        }
        val patcher = PackageNamePatcher(
            psiFactory,
            packageMapping,
            additionalBasePackage,
            transformHelpersPackage = true
        )
        ktFiles.values.forEach { it.accept(patcher, emptySet()) }
        for ((testFile, ktFile) in ktFiles) {
            filesContent[testFile] = ktFile.text
        }
    }

    override fun revert(file: TestFile, actualContent: String): String {
        var content = actualContent
        val additionalPackage = computePackage(testServices.testInfo)
        content = content.replace("@file:kotlin.native.internal.ReflectionPackageName(.*)\n".toRegex(), "")
        content = content.replace("package $additionalPackage\n", "")
        content = content.replace("$additionalPackage.", "")
        content = content.stripAdditionalEmptyLines(file)
        return content
    }

    private fun String.stripAdditionalEmptyLines(file: TestFile): String {
        return if (file.startLineNumberInOriginalFile != 0) {
            val prefix = (1 until file.startLineNumberInOriginalFile).joinToString(separator = "") { "\n" }
            this.removePrefix(prefix)
        } else {
            this
        }
    }

    private fun createPsiFactory(): KtPsiFactory {
        val configuration = CompilerConfiguration.create()

        val environment = KotlinCoreEnvironment.createForProduction(
            projectDisposable = testServices.compilerConfigurationProvider.testRootDisposable,
            configuration = configuration,
            configFiles = EnvironmentConfigFiles.METADATA_CONFIG_FILES
        )

        synchronized(lock) {
            CoreApplicationEnvironment.registerApplicationDynamicExtensionPoint(
                TreeCopyHandler.EP_NAME.name,
                TreeCopyHandler::class.java
            )
        }

        val project = environment.project as MockProject
        project.registerService(PomModel::class.java, PomModelImpl::class.java)
        project.registerService(TreeAspect::class.java)

        return KtPsiFactory(environment.project)
    }

    class PackageNamePatcher(
        val psiFactory: KtPsiFactory, // psiFactory
        val oldToNewPackageNameMapping: Map<FqName, FqName>,
        val basePackageName: FqName,
        val transformHelpersPackage: Boolean
    ) : KtVisitor<Unit, Set<Name>>() {
        companion object {
            private val HELPERS_PACKAGE_NAME = Name.identifier("helpers")
            private val KOTLINX_PACKAGE_NAME = Name.identifier("kotlinx")
            private val CNAMES_PACKAGE_NAME = Name.identifier("cnames")
            private val OBJCNAMES_PACKAGE_NAME = Name.identifier("objcnames")
            private val PLATFORM_PACKAGE_NAME = Name.identifier("platform")
        }

        override fun visitKtElement(element: KtElement, parentAccessibleDeclarationNames: Set<Name>) {
            element.getChildrenOfType<KtElement>().forEach { child ->
                child.accept(this, parentAccessibleDeclarationNames)
            }
        }

        override fun visitKtFile(file: KtFile, unused: Set<Name>) {
            // Patch package directive.
            val oldPackageDirective = file.packageDirective
            val oldPackageName = oldPackageDirective?.fqName ?: FqName.ROOT

            val newPackageName = oldToNewPackageNameMapping.getValue(file.packageFqNameForKLib)
            val newPackageDirective = psiFactory.createPackageDirective(newPackageName)

            if (oldPackageDirective != null) {
                // Replace old package directive by the new one.
                oldPackageDirective.replace(newPackageDirective).ensureSurroundedByNewLines()
            } else {
                // Insert the package directive immediately after file-level annotations.
                file.addAfter(newPackageDirective, file.fileAnnotationList).ensureSurroundedByNewLines()
            }

            if (!file.name.endsWith(".def")) { // don't process .def file contents after the package directive
                // Add @ReflectionPackageName annotation to make the compiler use the original package name in the reflective information.
                val annotationText =
                    "kotlin.native.internal.ReflectionPackageName(${oldPackageName.asString().quoteAsKotlinStringLiteral()})"
                val fileAnnotationList = psiFactory.createFileAnnotationListWithAnnotation(annotationText)
                file.addAnnotations(fileAnnotationList)

                visitKtElement(file, file.collectAccessibleDeclarationNames())
            }
        }

        override fun visitPackageDirective(directive: KtPackageDirective, unused: Set<Name>) = Unit

        override fun visitImportDirective(importDirective: KtImportDirective, unused: Set<Name>) {
            // Patch the import directive if necessary.
            val importedFqName = importDirective.importedFqName
            if (importedFqName == null
                || importedFqName.startsWith(StandardNames.BUILT_INS_PACKAGE_NAME)
                || importedFqName.startsWith(KOTLINX_PACKAGE_NAME)
                || (!transformHelpersPackage && importedFqName.startsWith(HELPERS_PACKAGE_NAME))
                || importedFqName.startsWith(CNAMES_PACKAGE_NAME)
                || importedFqName.startsWith(OBJCNAMES_PACKAGE_NAME)
                || importedFqName.startsWith(PLATFORM_PACKAGE_NAME)
            ) {
                return
            }

            val newImportPath = ImportPath(
                fqName = basePackageName.child(importedFqName),
                isAllUnder = importDirective.isAllUnder,
                alias = importDirective.aliasName?.let(Name::identifier)
            )
            importDirective.replace(psiFactory.createImportDirective(newImportPath))
        }

        override fun visitTypeAlias(typeAlias: KtTypeAlias, parentAccessibleDeclarationNames: Set<Name>) {
            super.visitTypeAlias(typeAlias, parentAccessibleDeclarationNames + typeAlias.collectAccessibleDeclarationNames())
        }

        override fun visitClassOrObject(classOrObject: KtClassOrObject, parentAccessibleDeclarationNames: Set<Name>) {
            super.visitClassOrObject(
                classOrObject,
                parentAccessibleDeclarationNames + classOrObject.collectAccessibleDeclarationNames()
            )
        }

        override fun visitClassBody(classBody: KtClassBody, parentAccessibleDeclarationNames: Set<Name>) {
            super.visitClassBody(classBody, parentAccessibleDeclarationNames + classBody.collectAccessibleDeclarationNames())
        }

        override fun visitPropertyAccessor(accessor: KtPropertyAccessor, parentAccessibleDeclarationNames: Set<Name>) {
            transformDeclarationWithBody(accessor, parentAccessibleDeclarationNames)
        }

        override fun visitNamedFunction(function: KtNamedFunction, parentAccessibleDeclarationNames: Set<Name>) {
            transformDeclarationWithBody(function, parentAccessibleDeclarationNames)
        }

        override fun visitPrimaryConstructor(constructor: KtPrimaryConstructor, parentAccessibleDeclarationNames: Set<Name>) {
            transformDeclarationWithBody(constructor, parentAccessibleDeclarationNames)
        }

        override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor, parentAccessibleDeclarationNames: Set<Name>) {
            transformDeclarationWithBody(constructor, parentAccessibleDeclarationNames)
        }

        private fun transformDeclarationWithBody(
            declarationWithBody: KtDeclarationWithBody,
            parentAccessibleDeclarationNames: Set<Name>,
        ) {
            val (expressions, nonExpressions) = declarationWithBody.getChildrenOfType<KtElement>().partition { it is KtExpression }

            val accessibleDeclarationNames =
                parentAccessibleDeclarationNames + declarationWithBody.collectAccessibleDeclarationNames()
            nonExpressions.forEach { it.accept(this, accessibleDeclarationNames) }

            val bodyAccessibleDeclarationNames =
                accessibleDeclarationNames + declarationWithBody.valueParameters.map { it.nameAsSafeName }
            expressions.forEach { it.accept(this, bodyAccessibleDeclarationNames) }
        }

        override fun visitExpression(expression: KtExpression, parentAccessibleDeclarationNames: Set<Name>) {
            if (expression is KtFunctionLiteral)
                transformDeclarationWithBody(expression, parentAccessibleDeclarationNames)
            else
                super.visitExpression(expression, parentAccessibleDeclarationNames)
        }

        override fun visitBlockExpression(expression: KtBlockExpression, parentAccessibleDeclarationNames: Set<Name>) {
            val accessibleDeclarationNames = parentAccessibleDeclarationNames.toMutableSet()
            expression.getChildrenOfType<KtElement>().forEach { child ->
                child.accept(this, accessibleDeclarationNames)
                accessibleDeclarationNames.addIfNotNull(child.name?.let(Name::identifier))
            }
        }

        override fun visitDotQualifiedExpression(
            dotQualifiedExpression: KtDotQualifiedExpression,
            accessibleDeclarationNames: Set<Name>,
        ) {
            val names = dotQualifiedExpression.collectNames()

            val newDotQualifiedExpression =
                visitPossiblyTypeReferenceWithFullyQualifiedName(names, accessibleDeclarationNames) { newPackageName ->
                    val newDotQualifiedExpression = psiFactory
                        .createFile("val x = ${newPackageName.asString()}.${dotQualifiedExpression.text}")
                        .getChildOfType<KtProperty>()!!
                        .getChildOfType<KtDotQualifiedExpression>()!!

                    dotQualifiedExpression.rawReplace(newDotQualifiedExpression) as KtDotQualifiedExpression
                } ?: dotQualifiedExpression

            super.visitDotQualifiedExpression(newDotQualifiedExpression, accessibleDeclarationNames)
        }

        override fun visitUserType(userType: KtUserType, accessibleDeclarationNames: Set<Name>) {
            val names = userType.collectNames()

            val newUserType =
                visitPossiblyTypeReferenceWithFullyQualifiedName(names, accessibleDeclarationNames) { newPackageName ->
                    val newUserType = psiFactory
                        .createFile("val x: ${newPackageName.asString()}.${userType.text}")
                        .getChildOfType<KtProperty>()!!
                        .getChildOfType<KtTypeReference>()!!
                        .typeElement as KtUserType

                    userType.replace(newUserType) as KtUserType
                } ?: userType

            newUserType.typeArgumentList?.let { visitKtElement(it, accessibleDeclarationNames) }
        }

        private fun <T : KtElement> visitPossiblyTypeReferenceWithFullyQualifiedName(
            names: List<Name>,
            accessibleDeclarationNames: Set<Name>,
            action: (newSubPackageName: FqName) -> T,
        ): T? {
            if (names.size < 2) return null

            if (names.first() in accessibleDeclarationNames) return null

            for (index in 1 until names.size) {
                val subPackageName = names.fqNameBeforeIndex(index)
                val newPackageName = oldToNewPackageNameMapping[subPackageName]
                if (newPackageName != null)
                    return action(newPackageName.removeSuffix(subPackageName))
            }

            return null
        }
    }
}

fun FqName.child(child: FqName): FqName {
    return child.pathSegments().fold(this) { accumulator, segment -> accumulator.child(segment) }
}

fun KtFile.addAnnotations(fileAnnotationList: KtFileAnnotationList) {
    val oldFileAnnotationList = this.fileAnnotationList
    if (oldFileAnnotationList != null) {
        // Add new annotations to the old ones.
        fileAnnotationList.annotationEntries.forEach {
            oldFileAnnotationList.add(it).ensureSurroundedByNewLines()
        }
    } else {
        // Insert the annotations list immediately before the package directive.
        this.addBefore(fileAnnotationList, packageDirective).ensureSurroundedByNewLines()
    }
}

val KtFile.packageFqNameForKLib: FqName
    get() = when (name.substringAfterLast(".")) {
        "kt" -> packageFqName
        "def" -> {
            // Without a package directive, CInterop tool puts declarations to a package with kinda odd name, as such:
            // name of .def file without extension, split by dot-separated parts, and reversed.
            if (packageFqName != FqName.ROOT) packageFqName
            else FqName.fromSegments(name.removeSuffix(".def").split(".").reversed())
        }
        else -> TODO("File extension is not yet supported: $name")
    }

internal fun PsiElement.ensureSurroundedByNewLines(): PsiElement =
    ensureHasNewLineBefore().ensureHasNewLineAfter()

private fun PsiElement.ensureHasNewLineBefore(): PsiElement {
    val (fileBoundaryReached, whiteSpaceBefore) = whiteSpaceBefore()
    if (!fileBoundaryReached and !whiteSpaceBefore.endsWith("\n")) {
        parent.addBefore(KtPsiFactory(project).createWhiteSpace("\n"), this)
    }
    return this
}

private fun PsiElement.ensureHasNewLineAfter(): PsiElement {
    val (fileBoundaryReached, whiteSpaceAfter) = whiteSpaceAfter()
    if (!fileBoundaryReached and !whiteSpaceAfter.startsWith("\n")) {
        parent.addAfter(KtPsiFactory(project).createWhiteSpace("\n"), this)
    }
    return this
}

private fun PsiElement.whiteSpaceBefore(): Pair<Boolean, String> {
    var fileBoundaryReached = false

    fun PsiElement.prevWhiteSpace(): PsiWhiteSpace? = when (val prevLeaf = prevLeaf(skipEmptyElements = true)) {
        null -> {
            fileBoundaryReached = true
            null
        }
        else -> prevLeaf as? PsiWhiteSpace
    }

    val whiteSpace = buildString {
        generateSequence(prevWhiteSpace()) { it.prevWhiteSpace() }.toList().asReversed().forEach { append(it.text) }
    }

    return fileBoundaryReached to whiteSpace
}

private fun PsiElement.whiteSpaceAfter(): Pair<Boolean, String> {
    var fileBoundaryReached = false

    fun PsiElement.nextWhiteSpace(): PsiWhiteSpace? = when (val nextLeaf = nextLeaf(skipEmptyElements = true)) {
        null -> {
            fileBoundaryReached = true
            null
        }
        else -> nextLeaf as? PsiWhiteSpace
    }

    val whiteSpace = buildString {
        generateSequence(nextWhiteSpace()) { it.nextWhiteSpace() }.forEach { append(it.text) }
    }

    return fileBoundaryReached to whiteSpace
}

/**
 * Returns the expression to be parsed by Kotlin as a string literal with given contents.
 * i.e. transforms `foo$bar` to `"foo\$bar"`.
 */
private fun String.quoteAsKotlinStringLiteral(): String = buildString {
    append('"')

    this@quoteAsKotlinStringLiteral.forEach { c ->
        when (c) {
            in charactersAllowedInKotlinStringLiterals -> append(c)
            '$' -> append("\\$")
            else -> append("\\u" + "%04X".format(c.code))
        }
    }

    append('"')
}

private val charactersAllowedInKotlinStringLiterals: Set<Char> = mutableSetOf<Char>().apply {
    addAll('a'..'z')
    addAll('A'..'Z')
    addAll('0'..'9')
    addAll(listOf('_', '@', ':', ';', '.', ',', '{', '}', '=', '[', ']', '^', '#', '*', ' ', '(', ')'))
}

private fun KtElement.collectAccessibleDeclarationNames(): Set<Name> {
    val names = hashSetOf<Name>()

    if (this is KtTypeParameterListOwner) {
        typeParameters.mapTo(names) { it.nameAsSafeName }
    }

    children.forEach { child ->
        if (child is KtNamedDeclaration) {
            when (child) {
                is KtClassLikeDeclaration,
                is KtVariableDeclaration,
                is KtParameter,
                is KtTypeParameter,
                    -> names += child.nameAsSafeName
            }
        }

        if (child is KtDestructuringDeclaration) {
            child.entries.mapTo(names) { it.nameAsSafeName }
        }
    }

    return names
}

private fun KtDotQualifiedExpression.collectNames(): List<Name> {
    val output = mutableListOf<Name>()

    fun KtExpression.recurse(): Boolean {
        children.forEach { child ->
            when (child) {
                is KtExpression -> when (child) {
                    is KtDotQualifiedExpression -> if (!child.recurse()) return false
                    is KtCallExpression,
                    is KtArrayAccessExpression,
                    is KtClassLiteralExpression,
                    is KtPostfixExpression,
                        -> {
                        child.recurse()
                        return false
                    }
                    is KtCallableReferenceExpression -> {
                        // 'T' from 'T::foo' should be considered, '::foo' should be discarded.
                        child.getChildrenOfType<KtNameReferenceExpression>()
                            .takeIf { it.size == 2 }
                            ?.first()
                            ?.let { output += it.getReferencedNameAsName() }
                        return false
                    }
                    is KtSafeQualifiedExpression -> {
                        // Consider only the first KtNameReferenceExpression child.
                        output.addIfNotNull(child.getChildOfType<KtNameReferenceExpression>()?.getReferencedNameAsName())
                        return false
                    }
                    is KtNameReferenceExpression -> output += child.getReferencedNameAsName()
                    else -> return false
                }
                else -> return false
            }
        }
        return true
    }

    recurse()
    return output
}

private fun KtUserType.collectNames(output: MutableList<Name> = mutableListOf()): List<Name> {
    children.forEach { child ->
        when (child) {
            is KtUserType -> child.collectNames(output)
            is KtNameReferenceExpression -> output += child.getReferencedNameAsName()
            else -> Unit
        }
    }

    return output
}

private fun List<Name>.fqNameBeforeIndex(toIndexExclusive: Int): FqName =
    if (toIndexExclusive == 0) FqName.ROOT else FqName(subList(0, toIndexExclusive).joinToString("."))

private fun FqName.removeSuffix(suffix: FqName): FqName {
    val pathSegments = pathSegments()
    val suffixPathSegments = suffix.pathSegments()

    val suffixStart = pathSegments.size - suffixPathSegments.size
    check(suffixPathSegments == pathSegments.subList(suffixStart, pathSegments.size))

    return FqName(pathSegments.take(suffixStart).joinToString("."))
}
