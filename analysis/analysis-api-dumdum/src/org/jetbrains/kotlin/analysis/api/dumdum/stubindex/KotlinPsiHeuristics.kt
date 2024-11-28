// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.analysis.api.dumdum.stubindex

import com.google.common.collect.HashMultimap
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

@OptIn(UnsafeCastFunction::class)
object KotlinPsiHeuristics {
    @JvmStatic
    fun unwrapImportAlias(file: KtFile, aliasName: String): Collection<String> {
        if (!file.hasImportAlias()) return emptyList()

        return file.aliasImportMap[aliasName]
    }

    @JvmStatic
    fun unwrapImportAlias(type: KtUserType, aliasName: String): Collection<String> {
        val file = type.containingKotlinFileStub?.psi as? KtFile ?: return emptyList()
        return unwrapImportAlias(file, aliasName)
    }

    @JvmStatic
    fun getImportAliases(file: KtFile, names: Set<String>): Set<String> {
        if (!file.hasImportAlias()) return emptySet()

        val result = LinkedHashSet<String>()
        for ((aliasName, name) in file.aliasImportMap.entries()) {
            if (name in names) {
                result += aliasName
            }
        }
        return result
    }

    private val KtFile.aliasImportMap by userDataCached("ALIAS_IMPORT_MAP_KEY") { file ->
        HashMultimap.create<String, String>().apply {
            for (import in file.importList?.imports.orEmpty()) {
                val aliasName = import.aliasName ?: continue
                val name = import.importPath?.fqName?.shortName()?.asString() ?: continue
                put(aliasName, name)
            }
        }
    }

    @JvmStatic
    fun isProbablyNothing(typeReference: KtTypeReference): Boolean {
        val userType = typeReference.typeElement as? KtUserType ?: return false
        return isProbablyNothing(userType)
    }

    @JvmStatic
    fun isProbablyNothing(type: KtUserType): Boolean {
        val referencedName = type.referencedName

        if (referencedName == "Nothing") {
            return true
        }

        // TODO: why don't use PSI-less stub for calculating aliases?
        val file = type.containingKotlinFileStub?.psi as? KtFile ?: return false

        // TODO: support type aliases
        if (!file.hasImportAlias()) return false
        return file.aliasImportMap[referencedName].contains("Nothing")
    }

    @JvmStatic
    fun getJvmName(fqName: FqName): String {
        val asString = fqName.asString()
        var startIndex = 0
        while (startIndex != -1) { // always true
            val dotIndex = asString.indexOf('.', startIndex)
            if (dotIndex == -1) return asString

            startIndex = dotIndex + 1
            val charAfterDot = asString.getOrNull(startIndex) ?: return asString
            if (!charAfterDot.isLetter()) return asString
            if (charAfterDot.isUpperCase()) return buildString {
                append(asString.subSequence(0, startIndex))
                append(asString.substring(startIndex).replace('.', '$'))
            }
        }

        return asString
    }

    @JvmStatic
    fun getPackageName(file: KtFile): FqName? {
        val entry = JvmFileClassUtil.findAnnotationEntryOnFileNoResolve(file, JvmStandardClassIds.JVM_PACKAGE_NAME_SHORT) ?: return null
        val customPackageName = JvmFileClassUtil.getLiteralStringFromAnnotation(entry)
        if (customPackageName != null) {
            return FqName(customPackageName)
        }

        return file.packageFqName
    }

    @JvmStatic
    fun getJvmName(declaration: KtClassOrObject): String? {
        val classId = declaration.classIdIfNonLocal ?: return null
        val jvmClassName = JvmClassName.byClassId(classId)
        return jvmClassName.fqNameForTopLevelClassMaybeWithDollars.asString()
    }

    private fun checkAnnotationUseSiteTarget(annotationEntry: KtAnnotationEntry, useSiteTarget: AnnotationUseSiteTarget?): Boolean {
        return useSiteTarget == null || annotationEntry.useSiteTarget?.getAnnotationUseSiteTarget() == useSiteTarget
    }

    @JvmStatic
    fun findAnnotation(declaration: KtAnnotated, shortName: String, useSiteTarget: AnnotationUseSiteTarget? = null): KtAnnotationEntry? {
        return declaration.annotationEntries
            .firstOrNull { checkAnnotationUseSiteTarget(it, useSiteTarget) && it.shortName?.asString() == shortName }
    }

    @JvmStatic
    fun findAnnotation(declaration: KtAnnotated, fqName: FqName, useSiteTarget: AnnotationUseSiteTarget? = null): KtAnnotationEntry? {
        val targetShortName = fqName.shortName().asString()
        val targetAliasName = declaration.containingKtFile.findAliasByFqName(fqName)?.name
        return findAnnotationInList(declaration.annotationEntries, targetShortName, targetAliasName, useSiteTarget)
    }

    @JvmStatic
    fun findAnnotation(
        fileAnnotationList: KtFileAnnotationList,
        fqName: FqName,
        useSiteTarget: AnnotationUseSiteTarget? = null
    ): KtAnnotationEntry? {
        val targetShortName = fqName.shortName().asString()
        val targetAliasName = fileAnnotationList.containingKtFile.findAliasByFqName(fqName)?.name
        return findAnnotationInList(fileAnnotationList.annotationEntries, targetShortName, targetAliasName, useSiteTarget)
    }

    @JvmStatic
    fun findAnnotationInList(
        annotationEntries: List<KtAnnotationEntry>,
        targetShortName: String,
        targetAliasName: String?,
        useSiteTarget: AnnotationUseSiteTarget? = null
    ): KtAnnotationEntry? {
        for (annotationEntry in annotationEntries) {
            if (!checkAnnotationUseSiteTarget(annotationEntry, useSiteTarget)) {
                continue
            }
            val annotationShortName = annotationEntry.shortName?.asString() ?: continue
            if (annotationShortName == targetShortName || annotationShortName == targetAliasName) {
                return annotationEntry
            }
        }
        return null
    }

    @JvmStatic
    fun hasAnnotation(declaration: KtAnnotated, shortName: String, useSiteTarget: AnnotationUseSiteTarget? = null): Boolean {
        return findAnnotation(declaration, shortName, useSiteTarget) != null
    }

    @JvmStatic
    fun hasAnnotation(declaration: KtAnnotated, shortName: Name, useSiteTarget: AnnotationUseSiteTarget? = null): Boolean {
        return findAnnotation(declaration, shortName.asString(), useSiteTarget) != null
    }

    @JvmStatic
    fun hasAnnotation(declaration: KtAnnotated, fqName: FqName, useSiteTarget: AnnotationUseSiteTarget? = null): Boolean {
        return findAnnotation(declaration, fqName, useSiteTarget) != null
    }

    @JvmStatic
    fun findJvmName(declaration: KtAnnotated, useSiteTarget: AnnotationUseSiteTarget? = null): String? {
        val annotation = findAnnotation(declaration, JvmFileClassUtil.JVM_NAME, useSiteTarget) ?: return null
        return JvmFileClassUtil.getLiteralStringFromAnnotation(annotation)
    }

    @JvmStatic
    fun findJvmGetterName(declaration: KtValVarKeywordOwner): String? {
        return when (declaration) {
            is KtProperty -> declaration.getter?.let(KotlinPsiHeuristics::findJvmName) ?: findJvmName(declaration, AnnotationUseSiteTarget.PROPERTY_GETTER)
            is KtParameter -> findJvmName(declaration, AnnotationUseSiteTarget.PROPERTY_GETTER)
            else -> null
        }
    }

    @JvmStatic
    fun findJvmSetterName(declaration: KtValVarKeywordOwner): String? {
        return when (declaration) {
            is KtProperty -> declaration.setter?.let(KotlinPsiHeuristics::findJvmName) ?: findJvmName(declaration, AnnotationUseSiteTarget.PROPERTY_SETTER)
            is KtParameter -> findJvmName(declaration, AnnotationUseSiteTarget.PROPERTY_SETTER)
            else -> null
        }
    }

    @JvmStatic
    fun findSuppressAnnotation(declaration: KtAnnotated): KtAnnotationEntry? {
        return findAnnotation(declaration, StandardNames.FqNames.suppress)
    }

    @JvmStatic
    fun hasSuppressAnnotation(declaration: KtAnnotated): Boolean {
        return findSuppressAnnotation(declaration) != null
    }

    @JvmStatic
    fun hasNonSuppressAnnotations(declaration: KtAnnotated): Boolean {
        val annotationEntries = declaration.annotationEntries
        return annotationEntries.size > 1 || annotationEntries.size == 1 && !hasSuppressAnnotation(declaration)
    }

    @JvmStatic
    fun hasJvmFieldAnnotation(declaration: KtAnnotated): Boolean {
        return hasAnnotation(declaration,  JvmAbi.JVM_FIELD_ANNOTATION_FQ_NAME)
    }

    @JvmStatic
    fun hasJvmOverloadsAnnotation(declaration: KtAnnotated): Boolean {
        return hasAnnotation(declaration, JvmStandardClassIds.JVM_OVERLOADS_FQ_NAME)
    }

    @JvmStatic
    fun hasJvmStaticAnnotation(declaration: KtAnnotated): Boolean {
        return hasAnnotation(declaration, JvmStandardClassIds.JVM_STATIC_FQ_NAME)
    }

    private val PUBLISHED_API_FQN = FqName("kotlin.PublishedApi")

    @JvmStatic
    fun hasPublishedApiAnnotation(declaration: KtAnnotated): Boolean {
        return hasAnnotation(declaration, PUBLISHED_API_FQN)
    }

    @JvmStatic
    fun getStringValue(argument: ValueArgument): String? {
        return argument.getArgumentExpression()
            ?.safeAs<KtStringTemplateExpression>()
            ?.entries
            ?.singleOrNull()
            ?.safeAs<KtLiteralStringTemplateEntry>()
            ?.text
    }

    @JvmStatic
    fun findSuppressedEntities(declaration: KtAnnotated): List<String>? {
        val entry = findSuppressAnnotation(declaration) ?: return null
        return entry.valueArguments.mapNotNull(KotlinPsiHeuristics::getStringValue)
    }

    @JvmStatic
    fun isPossibleOperator(declaration: KtNamedFunction): Boolean {
        if (declaration.hasModifier(KtTokens.OPERATOR_KEYWORD)) {
            return true
        } else if (!declaration.hasModifier(KtTokens.OVERRIDE_KEYWORD)) {
            // Operator modifier could be omitted only for overridden function
            return false
        }

        val name = declaration.name ?: return false
        return OperatorConventions.isConventionName(Name.identifier(name))
    }

    /**
     * Performs simple PSI-only type equivalence check. Might be useful for preliminary checks.
     * Only `KtUserType` types are checked. Nullability is not supported.
     *
     * Examples:
     * - `typeMatches("kotlin.Array", "String")`
     * - `typeMatches("MutableMap", "Key", "out Value")`
     */
    fun typeMatches(typeReference: KtTypeReference, name: ClassId, vararg args: ClassId): Boolean {
        val typeElement = typeReference.typeElement ?: return false
        return typeMatches(typeElement, name, *args)
    }

    private fun typeMatches(typeElement: KtTypeElement, name: ClassId, vararg args: ClassId): Boolean {
        fun checkName(typeElement: KtUserType, expectedName: ClassId): Boolean {
            val actualChunks = generateSequence(typeElement) { it.qualifier }
                .map { it.referencedName }
                .toList().asReversed()

            val expectedChunks = sequenceOf(expectedName.packageFqName, expectedName.relativeClassName)
                .flatMap { it.pathSegments() }
                .map { it.asString() }
                .toList()

            // 'kotlin.Unit' should match 'Unit', yet not 'foo.kotlin.Unit'
            return expectedChunks.size >= actualChunks.size
                    && expectedChunks.subList(expectedChunks.size - actualChunks.size, expectedChunks.size) == actualChunks
        }

        if (typeElement is KtNullableType) {
            val innerType = typeElement.innerType
            return innerType != null && typeMatches(innerType, name, *args)
        }

        if (typeElement is KtUserType) {
            if (!checkName(typeElement, name)) {
                return false
            }

            val typeArguments = typeElement.typeArguments
            if (args.size != typeArguments.size) {
                return false
            }

            for ((index, typeArgument) in typeArguments.withIndex()) {
                if (typeArgument.projectionKind == KtProjectionKind.STAR) {
                    return false
                }

                val argTypeElement = typeArgument.typeReference?.typeElement
                if (argTypeElement == null || !typeMatches(argTypeElement, args[index])) {
                    return false
                }
            }
        }

        return true
    }

    fun isEnumCompanionPropertyWithEntryConflict(element: PsiElement, expectedName: String): Boolean {
        if (element !is KtProperty) return false

        val propertyClass = element.containingClassOrObject as? KtObjectDeclaration ?: return false
        if (!propertyClass.isCompanion()) return false

        val outerClass = propertyClass.containingClassOrObject as? KtClass ?: return false
        if (!outerClass.isEnum()) return false

        return outerClass.declarations.any { it is KtEnumEntry && it.name == expectedName }
    }
}