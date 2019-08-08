/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.expectactual

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaDirectoryService
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.backend.common.descriptors.explicitParameters
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.project.implementedDescriptors
import org.jetbrains.kotlin.idea.caches.project.isTestModule
import org.jetbrains.kotlin.idea.caches.project.toDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithContent
import org.jetbrains.kotlin.idea.codeInsight.shorten.addToBeShortenedDescendantsToWaitingSet
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.findOrCreateDirectoryForPackage
import org.jetbrains.kotlin.idea.core.getFqNameWithImplicitPrefix
import org.jetbrains.kotlin.idea.core.overrideImplement.MemberGenerateMode
import org.jetbrains.kotlin.idea.core.overrideImplement.OverrideMemberChooserObject.BodyType.EMPTY_OR_TEMPLATE
import org.jetbrains.kotlin.idea.core.overrideImplement.OverrideMemberChooserObject.BodyType.NO_BODY
import org.jetbrains.kotlin.idea.core.overrideImplement.OverrideMemberChooserObject.Companion.create
import org.jetbrains.kotlin.idea.core.overrideImplement.generateMember
import org.jetbrains.kotlin.idea.core.overrideImplement.makeNotActual
import org.jetbrains.kotlin.idea.core.toDescriptor
import org.jetbrains.kotlin.idea.project.platform
import org.jetbrains.kotlin.idea.refactoring.createKotlinFile
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.idea.util.*
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.hasActualModifier
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.checkers.ExpectedActualDeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.ExperimentalUsageChecker
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.AbbreviatedType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun createFileForDeclaration(module: Module, declaration: KtNamedDeclaration): KtFile? {
    val fileName = declaration.name ?: return null

    val originalDir = declaration.containingFile.containingDirectory
    val containerPackage = JavaDirectoryService.getInstance().getPackage(originalDir)
    val packageDirective = declaration.containingKtFile.packageDirective
    val directory = findOrCreateDirectoryForPackage(
        module, containerPackage?.qualifiedName ?: ""
    ) ?: return null
    return runWriteAction {
        val fileNameWithExtension = "$fileName.kt"
        val existingFile = directory.findFile(fileNameWithExtension)
        val packageName =
            if (packageDirective?.packageNameExpression == null) directory.getFqNameWithImplicitPrefix()?.asString()
            else packageDirective.fqName.asString()
        if (existingFile is KtFile) {
            val existingPackageDirective = existingFile.packageDirective
            if (existingFile.declarations.isNotEmpty() &&
                existingPackageDirective?.fqName != packageDirective?.fqName
            ) {
                val newName = KotlinNameSuggester.suggestNameByName(fileName) {
                    directory.findFile("$it.kt") == null
                } + ".kt"
                createKotlinFile(newName, directory, packageName)
            } else {
                existingFile
            }
        } else {
            createKotlinFile(fileNameWithExtension, directory, packageName)
        }
    }
}

fun KtPsiFactory.createClassHeaderCopyByText(originalClass: KtClassOrObject): KtClassOrObject {
    val text = originalClass.text
    return when (originalClass) {
        is KtObjectDeclaration -> if (originalClass.isCompanion()) {
            createCompanionObject(text)
        } else {
            createObject(text)
        }
        is KtEnumEntry -> createEnumEntry(text)
        else -> createClass(text)
    }.apply {
        declarations.forEach(KtDeclaration::delete)
        primaryConstructor?.delete()
    }
}

fun KtNamedDeclaration?.getTypeDescription(): String = when (this) {
    is KtObjectDeclaration -> "object"
    is KtClass -> when {
        isInterface() -> "interface"
        isEnum() -> "enum class"
        isAnnotation() -> "annotation class"
        else -> "class"
    }
    is KtProperty, is KtParameter -> "property"
    is KtFunction -> "function"
    else -> "declaration"
}

internal fun KtPsiFactory.generateClassOrObject(
    project: Project,
    generateExpectClass: Boolean,
    originalClass: KtClassOrObject,
    targetModule: Module,
    outerClasses: List<KtClassOrObject> = emptyList(),
    existingFqNames: Set<String> = emptySet()
): KtClassOrObject {
    val generatedClass = createClassHeaderCopyByText(originalClass)
    val context = originalClass.analyzeWithContent()
    val superNames = repairSuperTypeList(
        generatedClass,
        originalClass,
        generateExpectClass,
        project,
        targetModule,
        existingFqNames,
        context
    )

    if (generatedClass.isAnnotation()) {
        generatedClass.annotationEntries.zip(originalClass.annotationEntries).forEach { (generatedEntry, originalEntry) ->
            val annotationDescriptor = context.get(BindingContext.ANNOTATION, originalEntry) ?: return@forEach
            if (annotationDescriptor.fqName in forbiddenAnnotationFqNames) {
                generatedEntry.delete()
            }
        }
    }
    if (generateExpectClass) {
        if (outerClasses.isEmpty()) {
            generatedClass.addModifier(KtTokens.EXPECT_KEYWORD)
        } else {
            generatedClass.makeNotActual()
        }
        generatedClass.removeModifier(KtTokens.DATA_KEYWORD)
    } else {
        if (generatedClass !is KtEnumEntry) {
            generatedClass.addModifier(KtTokens.ACTUAL_KEYWORD)
        }
    }

    val existingClasses = originalClass.declarations.asSequence().filterIsInstance<KtClassOrObject>().filter {
        it.isEffectivelyActual(false)
    }.toList() + generatedClass + outerClasses

    declLoop@ for (originalDeclaration in originalClass.declarations) {
        val descriptor = originalDeclaration.toDescriptor() ?: continue
        if (generateExpectClass && !originalDeclaration.isEffectivelyActual(false)) continue
        val generatedDeclaration: KtDeclaration = when (originalDeclaration) {
            is KtClassOrObject -> generateClassOrObject(
                project,
                generateExpectClass,
                originalDeclaration,
                targetModule,
                existingClasses,
                existingFqNames
            )
            is KtCallableDeclaration -> {
                when (originalDeclaration) {
                    is KtFunction -> generateFunction(
                        project,
                        generateExpectClass,
                        originalDeclaration,
                        descriptor as FunctionDescriptor,
                        generatedClass,
                        existingClasses,
                        existingFqNames + superNames
                    )
                    is KtProperty -> generateProperty(
                        project,
                        generateExpectClass,
                        originalDeclaration,
                        descriptor as PropertyDescriptor,
                        generatedClass,
                        existingClasses,
                        existingFqNames + superNames
                    )
                    else -> continue@declLoop
                }
            }
            else -> continue@declLoop
        }
        generatedClass.addDeclaration(generatedDeclaration)
    }
    if (!originalClass.isAnnotation() && !originalClass.hasInlineModifier()) {
        for (originalProperty in originalClass.primaryConstructorParameters) {
            if (!originalProperty.hasValOrVar() || !originalProperty.hasActualModifier()) continue
            val descriptor = originalProperty.toDescriptor() as? PropertyDescriptor ?: continue
            val generatedProperty = generateProperty(
                project,
                generateExpectClass,
                originalProperty,
                descriptor,
                generatedClass,
                outerClasses,
                existingFqNames + superNames
            )
            generatedClass.addDeclaration(generatedProperty)
        }
    }
    val originalPrimaryConstructor = originalClass.primaryConstructor
    if (
        generatedClass is KtClass
        && originalPrimaryConstructor != null
        && (!generateExpectClass || originalPrimaryConstructor.hasActualModifier())
    ) {
        val descriptor = originalPrimaryConstructor.toDescriptor()
        if (descriptor is FunctionDescriptor) {
            val expectedPrimaryConstructor = generateFunction(
                project,
                generateExpectClass,
                originalPrimaryConstructor,
                descriptor,
                generatedClass,
                outerClasses,
                existingFqNames + superNames
            )
            generatedClass.createPrimaryConstructorIfAbsent().replace(expectedPrimaryConstructor)
        }
    }

    return generatedClass
}

private fun KtPsiFactory.repairSuperTypeList(
    generated: KtClassOrObject,
    original: KtClassOrObject,
    generateExpectClass: Boolean,
    project: Project,
    targetModule: Module?,
    existingFqNames: Set<String>,
    context: BindingContext
): Collection<String> {
    val superNames = linkedSetOf<String>()
    generated.superTypeListEntries.zip(original.superTypeListEntries).forEach { (generatedEntry, originalEntry) ->
        val superType = context[BindingContext.TYPE, originalEntry.typeReference]
        val superClassDescriptor = superType?.constructor?.declarationDescriptor as? ClassDescriptor ?: return@forEach
        if (generateExpectClass
            && targetModule != null
            && !superType.checkTypeAccessibilityInModule(project, targetModule, existingFqNames)
        ) {
            generatedEntry.delete()
            return@forEach
        }

        superType.fqName?.shortName()?.asString()?.let { superNames += it }
        if (generateExpectClass) {
            if (generatedEntry !is KtSuperTypeCallEntry) return@forEach
        } else {
            if (generatedEntry !is KtSuperTypeEntry) return@forEach
        }

        if (superClassDescriptor.kind == ClassKind.CLASS || superClassDescriptor.kind == ClassKind.ENUM_CLASS) {
            val entryText = IdeDescriptorRenderers.SOURCE_CODE.renderType(superType)
            val newGeneratedEntry = if (generateExpectClass) {
                createSuperTypeEntry(entryText)
            } else {
                createSuperTypeCallEntry("$entryText()")
            }
            generatedEntry.replace(newGeneratedEntry).safeAs<KtElement>()?.addToBeShortenedDescendantsToWaitingSet()
        }
    }

    if (generated.superTypeListEntries.isEmpty()) generated.getSuperTypeList()?.delete()
    return superNames
}

private val forbiddenAnnotationFqNames = setOf(
    ExpectedActualDeclarationChecker.OPTIONAL_EXPECTATION_FQ_NAME,
    FqName("kotlin.ExperimentalMultiplatform"),
    ExperimentalUsageChecker.USE_EXPERIMENTAL_FQ_NAME
)

internal fun generateFunction(
    project: Project,
    generateExpect: Boolean,
    originalFunction: KtFunction,
    descriptor: FunctionDescriptor,
    generatedClass: KtClassOrObject? = null,
    outerClasses: List<KtClassOrObject> = emptyList(),
    existingFqNames: Collection<String> = emptySet()
): KtFunction {
    if (generateExpect) {
        val accessibleClasses = outerClasses + listOfNotNull(generatedClass)
        descriptor.checkTypeParameterBoundsAccessibility(accessibleClasses)
        descriptor.returnType?.checkAccessibility(accessibleClasses)
        descriptor.valueParameters.forEach {
            it.type.checkAccessibility(accessibleClasses)
        }
    }
    val memberChooserObject = create(
        originalFunction, descriptor, descriptor,
        if (generateExpect || descriptor.modality == Modality.ABSTRACT) NO_BODY else EMPTY_OR_TEMPLATE
    )
    return memberChooserObject.generateMember(
        targetClass = generatedClass,
        copyDoc = true,
        project = project,
        mode = if (generateExpect) MemberGenerateMode.EXPECT else MemberGenerateMode.ACTUAL
    ).also { if (generatedClass != null) it.repairOverride(descriptor, existingFqNames) } as KtFunction
}

internal fun generateProperty(
    project: Project,
    generateExpect: Boolean,
    originalProperty: KtNamedDeclaration,
    descriptor: PropertyDescriptor,
    generatedClass: KtClassOrObject? = null,
    outerClasses: List<KtClassOrObject> = emptyList(),
    existingFqNames: Collection<String> = emptySet()
): KtProperty {
    if (generateExpect) {
        val accessibleClasses = outerClasses + listOfNotNull(generatedClass)
        descriptor.checkTypeParameterBoundsAccessibility(accessibleClasses)
        descriptor.type.checkAccessibility(accessibleClasses)
    }
    val memberChooserObject = create(
        originalProperty, descriptor, descriptor,
        if (generateExpect || descriptor.modality == Modality.ABSTRACT) NO_BODY else EMPTY_OR_TEMPLATE
    )
    return memberChooserObject.generateMember(
        targetClass = generatedClass,
        copyDoc = true,
        project = project,
        mode = if (generateExpect) MemberGenerateMode.EXPECT else MemberGenerateMode.ACTUAL
    ).also { if (generatedClass != null) it.repairOverride(descriptor, existingFqNames) } as KtProperty
}

private fun KtCallableDeclaration.repairOverride(descriptor: CallableDescriptor, existingFqNames: Collection<String>) {
    if (!hasModifier(KtTokens.OVERRIDE_KEYWORD)) return

    val superDescriptor = descriptor.overriddenDescriptors.firstOrNull()?.containingDeclaration
    if (superDescriptor?.fqNameOrNull()?.shortName()?.asString() !in existingFqNames) {
        removeModifier(KtTokens.OVERRIDE_KEYWORD)
    }
}

private fun CallableMemberDescriptor.checkTypeParameterBoundsAccessibility(accessibleClasses: List<KtClassOrObject>) {
    typeParameters.forEach { typeParameter ->
        typeParameter.upperBounds.forEach { upperBound ->
            upperBound.checkAccessibility(accessibleClasses)
        }
    }
}

private fun KotlinType.checkAccessibility(accessibleClasses: List<KtClassOrObject>) {
    for (argument in arguments) {
        if (argument.isStarProjection) continue
        argument.type.checkAccessibility(accessibleClasses)
    }
    val classifierDescriptor = constructor.declarationDescriptor as? ClassifierDescriptorWithTypeParameters ?: return
    val moduleDescriptor = classifierDescriptor.module
    if (moduleDescriptor.platform.isCommon()) {
        // Common classes are Ok; unfortunately this check does not work correctly for simple (non-expect) classes from common module
        return
    }
    val declaration = DescriptorToSourceUtils.descriptorToDeclaration(classifierDescriptor)
    if (declaration?.module?.platform?.isCommon() == true) {
        // Common classes are Ok again
        return
    }
    val implementedDescriptors = moduleDescriptor.implementedDescriptors
    if (implementedDescriptors.isEmpty()) {
        // This happens now if we are not in sources, in this case yet we cannot answer question about accessibility
        // Very rude check about JDK classes
        if (!classifierDescriptor.fqNameSafe.toString().startsWith("java.")) return
    }
    if (implementedDescriptors.any { it.hasDeclarationOf(classifierDescriptor) }) {
        // Platform classes with expected class are also Ok
        return
    }
    accessibleClasses.forEach {
        if (classifierDescriptor.name == it.nameAsName) return
    }
    if (this is AbbreviatedType) {
        // For type aliases without expected class, check expansions instead
        expandedType.checkAccessibility(accessibleClasses)
    } else {
        throw KotlinTypeInaccessibleException(this)
    }
}

class KotlinTypeInaccessibleException(val type: KotlinType) : Exception() {
    override val message: String
        get() = "Type ${type.getJetTypeFqName(true)} is not accessible from common code"
}

fun DeclarationDescriptor.checkTypeAccessibilityInModule(
    project: Project,
    module: Module,
    existingClasses: Set<String> = emptySet()
): Boolean = checkTypeInSequence(collectAllTypes(), project, module, additionalClasses(existingClasses))

fun KotlinType.checkTypeAccessibilityInModule(
    project: Project,
    module: Module,
    existingClasses: Set<String> = emptySet()
): Boolean = checkTypeInSequence(collectAllTypes(), project, module, existingClasses)

private fun checkTypeInSequence(sequence: Sequence<FqName?>, project: Project, module: Module, existingClasses: Set<String>): Boolean {
    val builtInsModule = module.toDescriptor()
    val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, module.isTestModule)
    return sequence.all { fqName ->
        if (fqName == null) return false
        builtInsModule?.resolveClassByFqName(fqName, NoLookupLocation.FROM_BUILTINS) != null || fqName.canFindClassInModule(
            project,
            scope,
            existingClasses
        )
    }
}

private tailrec fun DeclarationDescriptor.additionalClasses(existingClasses: Set<String> = emptySet()): Set<String> = when (this) {
    is ClassifierDescriptorWithTypeParameters -> {
        val myParameters = existingClasses + declaredTypeParameters.map { it.fqNameOrNull()?.asString() ?: return emptySet() }
        val containingDeclaration = containingDeclaration
        if (isInner) containingDeclaration.additionalClasses(myParameters) else myParameters
    }
    is CallableDescriptor -> containingDeclaration.additionalClasses(existingClasses + typeParameters.map {
        it.fqNameOrNull()?.asString() ?: return emptySet()
    })
    else ->
        existingClasses
}

private fun DeclarationDescriptor.collectAllTypes(): Sequence<FqName?> {
    return when (this) {
        is ClassConstructorDescriptor -> valueParameters.asSequence().map(ValueParameterDescriptor::getType).flatMap(KotlinType::collectAllTypes)
        is ClassDescriptor -> if (isInline) unsubstitutedPrimaryConstructor?.collectAllTypes().orEmpty() else {
            emptySequence()
        } + declaredTypeParameters.asSequence().flatMap(DeclarationDescriptor::collectAllTypes) + sequenceOf(fqNameOrNull())
        is CallableDescriptor -> {
            val returnType = returnType ?: return sequenceOf(null)
            returnType.collectAllTypes() + explicitParameters.asSequence().map(ParameterDescriptor::getType).flatMap(KotlinType::collectAllTypes)
        }
        is TypeParameterDescriptor -> {
            val upperBounds = upperBounds
            if (upperBounds.isEmpty()) sequenceOf(fqNameOrNull())
            else upperBounds.asSequence().flatMap(KotlinType::collectAllTypes)
        }
        else -> emptySequence()
    }
}

private fun KotlinType.collectAllTypes(): Sequence<FqName?> = sequenceOf(fqName) + arguments.asSequence()
    .map(TypeProjection::getType)
    .flatMap(KotlinType::collectAllTypes)

fun KtNamedDeclaration.isAlwaysActual(): Boolean = safeAs<KtParameter>()?.parent?.parent?.safeAs<KtPrimaryConstructor>()
    ?.mustHaveValOrVar() ?: false

fun KtNamedDeclaration.checkTypeAccessibilityInModule(module: Module, existingClasses: Set<String> = emptySet()): Boolean {
    return !hasPrivateModifier() && descriptor?.checkTypeAccessibilityInModule(project, module, existingClasses) == true
}

val KotlinType.fqName: FqName? get() = constructor.declarationDescriptor?.fqNameOrNull()

private fun FqName.canFindClassInModule(project: Project, scope: GlobalSearchScope, existedClasses: Set<String>): Boolean {
    val name = asString()
    return name in existedClasses || KotlinFullClassNameIndex.getInstance()[name, project, scope].isNotEmpty()
}