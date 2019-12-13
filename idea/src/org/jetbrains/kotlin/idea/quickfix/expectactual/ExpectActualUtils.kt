/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.expectactual

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.JavaDirectoryService
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
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
import org.jetbrains.kotlin.idea.inspections.findExistingEditor
import org.jetbrains.kotlin.idea.quickfix.TypeAccessibilityChecker
import org.jetbrains.kotlin.idea.refactoring.createKotlinFile
import org.jetbrains.kotlin.idea.refactoring.fqName.fqName
import org.jetbrains.kotlin.idea.refactoring.introduce.showErrorHint
import org.jetbrains.kotlin.idea.refactoring.isInterfaceClass
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.idea.util.hasInlineModifier
import org.jetbrains.kotlin.idea.util.isEffectivelyActual
import org.jetbrains.kotlin.idea.util.mustHaveValOrVar
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.hasActualModifier
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.checkers.ExpectedActualDeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.ExperimentalUsageChecker
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.kotlin.types.KotlinType
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
    checker: TypeAccessibilityChecker
): KtClassOrObject {
    val generatedClass = createClassHeaderCopyByText(originalClass)
    val context = originalClass.analyzeWithContent()
    val superNames = repairSuperTypeList(
        generatedClass,
        originalClass,
        generateExpectClass,
        checker,
        context
    )

    generatedClass.annotationEntries.zip(originalClass.annotationEntries).forEach { (generatedEntry, originalEntry) ->
        val annotationDescriptor = context.get(BindingContext.ANNOTATION, originalEntry)
        if (annotationDescriptor?.isValidInModule(checker) != true) {
            generatedEntry.delete()
        }
    }

    if (generateExpectClass) {
        if (originalClass.isTopLevel()) {
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

    val existingFqNamesWithSuperTypes = (checker.existingTypeNames + superNames).toSet()
    declLoop@ for (originalDeclaration in originalClass.declarations) {
        val descriptor = originalDeclaration.toDescriptor() ?: continue
        if (generateExpectClass && !originalDeclaration.isEffectivelyActual(false)) continue
        val generatedDeclaration: KtDeclaration = when (originalDeclaration) {
            is KtClassOrObject -> generateClassOrObject(
                project,
                generateExpectClass,
                originalDeclaration,
                checker
            )
            is KtFunction, is KtProperty -> checker.runInContext(existingFqNamesWithSuperTypes) {
                generateCallable(
                    project,
                    generateExpectClass,
                    originalDeclaration as KtCallableDeclaration,
                    descriptor as CallableMemberDescriptor,
                    generatedClass,
                    this
                )
            }
            else -> continue@declLoop
        }
        generatedClass.addDeclaration(generatedDeclaration)
    }
    if (!originalClass.isAnnotation() && !originalClass.hasInlineModifier()) {
        for (originalProperty in originalClass.primaryConstructorParameters) {
            if (!originalProperty.hasValOrVar() || !originalProperty.hasActualModifier()) continue
            val descriptor = originalProperty.toDescriptor() as? PropertyDescriptor ?: continue
            checker.runInContext(existingFqNamesWithSuperTypes) {
                val generatedProperty = generateCallable(
                    project,
                    generateExpectClass,
                    originalProperty,
                    descriptor,
                    generatedClass,
                    this
                )
                generatedClass.addDeclaration(generatedProperty)
            }
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
            checker.runInContext(existingFqNamesWithSuperTypes) {
                val expectedPrimaryConstructor = generateCallable(
                    project,
                    generateExpectClass,
                    originalPrimaryConstructor,
                    descriptor,
                    generatedClass,
                    this
                )
                generatedClass.createPrimaryConstructorIfAbsent().replace(expectedPrimaryConstructor)
            }
        }
    }

    return generatedClass
}

private fun KtPsiFactory.repairSuperTypeList(
    generated: KtClassOrObject,
    original: KtClassOrObject,
    generateExpectClass: Boolean,
    checker: TypeAccessibilityChecker,
    context: BindingContext
): Collection<String> {
    val superNames = linkedSetOf<String>()
    val typeParametersFqName = context[BindingContext.DECLARATION_TO_DESCRIPTOR, original]
        ?.safeAs<ClassDescriptor>()
        ?.declaredTypeParameters?.mapNotNull { it.fqNameOrNull()?.asString() }.orEmpty()

    checker.runInContext(checker.existingTypeNames + typeParametersFqName) {
        generated.superTypeListEntries.zip(original.superTypeListEntries).forEach { (generatedEntry, originalEntry) ->
            val superType = context[BindingContext.TYPE, originalEntry.typeReference]
            val superClassDescriptor = superType?.constructor?.declarationDescriptor as? ClassDescriptor ?: return@forEach
            if (generateExpectClass && !checker.checkAccessibility(superType)) {
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
    }
    if (generated.superTypeListEntries.isEmpty()) generated.getSuperTypeList()?.delete()
    return superNames
}

private val forbiddenAnnotationFqNames = setOf(
    ExpectedActualDeclarationChecker.OPTIONAL_EXPECTATION_FQ_NAME,
    FqName("kotlin.ExperimentalMultiplatform"),
    ExperimentalUsageChecker.OPT_IN_FQ_NAME,
    ExperimentalUsageChecker.OLD_USE_EXPERIMENTAL_FQ_NAME
)

internal fun generateCallable(
    project: Project,
    generateExpect: Boolean,
    originalDeclaration: KtDeclaration,
    descriptor: CallableMemberDescriptor,
    generatedClass: KtClassOrObject? = null,
    checker: TypeAccessibilityChecker
): KtCallableDeclaration {
    descriptor.checkAccessibility(checker)
    val memberChooserObject = create(
        originalDeclaration, descriptor, descriptor,
        if (generateExpect || descriptor.modality == Modality.ABSTRACT) NO_BODY else EMPTY_OR_TEMPLATE
    )
    return memberChooserObject.generateMember(
        targetClass = generatedClass,
        copyDoc = true,
        project = project,
        mode = if (generateExpect) MemberGenerateMode.EXPECT else MemberGenerateMode.ACTUAL
    ).apply {
        repair(generatedClass, descriptor, checker)
    }
}

private fun CallableMemberDescriptor.checkAccessibility(checker: TypeAccessibilityChecker) {
    val errors = checker.incorrectTypes(this).ifEmpty { return }
    throw KotlinTypeInaccessibleException(errors.toSet())
}

private fun KtCallableDeclaration.repair(
    generatedClass: KtClassOrObject?,
    descriptor: CallableDescriptor,
    checker: TypeAccessibilityChecker
) {
    if (generatedClass != null) repairOverride(descriptor, checker)
    repairAnnotationEntries(this, descriptor, checker)
}

private fun KtCallableDeclaration.repairOverride(descriptor: CallableDescriptor, checker: TypeAccessibilityChecker) {
    if (!hasModifier(KtTokens.OVERRIDE_KEYWORD)) return

    val superDescriptor = descriptor.overriddenDescriptors.firstOrNull()?.containingDeclaration
    if (superDescriptor?.fqNameOrNull()?.shortName()?.asString() !in checker.existingTypeNames) {
        removeModifier(KtTokens.OVERRIDE_KEYWORD)
    }
}

private fun repairAnnotationEntries(
    target: KtModifierListOwner,
    descriptor: DeclarationDescriptorNonRoot,
    checker: TypeAccessibilityChecker
) {
    repairAnnotations(checker, target, descriptor.annotations)
    when (descriptor) {
        is ValueParameterDescriptor -> {
            if (target !is KtParameter) return
            val typeReference = target.typeReference ?: return
            repairAnnotationEntries(typeReference, descriptor.type, checker)
        }
        is TypeParameterDescriptor -> {
            if (target !is KtTypeParameter) return
            val extendsBound = target.extendsBound ?: return
            for (upperBound in descriptor.upperBounds) {
                repairAnnotationEntries(extendsBound, upperBound, checker)
            }
        }
        is CallableDescriptor -> {
            val extension = descriptor.extensionReceiverParameter
            val receiver = target.safeAs<KtCallableDeclaration>()?.receiverTypeReference
            if (extension != null && receiver != null) {
                repairAnnotationEntries(receiver, extension, checker)
            }

            val callableDeclaration = target.safeAs<KtCallableDeclaration>() ?: return
            callableDeclaration.typeParameters.zip(descriptor.typeParameters).forEach { (typeParameter, typeParameterDescriptor) ->
                repairAnnotationEntries(typeParameter, typeParameterDescriptor, checker)
            }

            callableDeclaration.valueParameters.zip(descriptor.valueParameters).forEach { (valueParameter, valueParameterDescriptor) ->
                repairAnnotationEntries(valueParameter, valueParameterDescriptor, checker)
            }
        }
    }
}

private fun repairAnnotationEntries(
    typeReference: KtTypeReference,
    type: KotlinType,
    checker: TypeAccessibilityChecker
) {
    repairAnnotations(checker, typeReference, type.annotations)
    typeReference.typeElement?.typeArgumentsAsTypes?.zip(type.arguments)?.forEach { (reference, projection) ->
        repairAnnotationEntries(reference, projection.type, checker)
    }
}

private fun repairAnnotations(checker: TypeAccessibilityChecker, target: KtModifierListOwner, annotations: Annotations) {
    for (annotation in annotations) {
        if (annotation.isValidInModule(checker)) {
            checkAndAdd(annotation, checker, target)
        }
    }
}

private fun checkAndAdd(annotationDescriptor: AnnotationDescriptor, checker: TypeAccessibilityChecker, target: KtModifierListOwner) {
    if (annotationDescriptor.isValidInModule(checker)) {
        val entry = annotationDescriptor.source.safeAs<KotlinSourceElement>()?.psi.safeAs<KtAnnotationEntry>() ?: return
        target.addAnnotationEntry(entry)
    }
}

private fun AnnotationDescriptor.isValidInModule(checker: TypeAccessibilityChecker): Boolean {
    return fqName !in forbiddenAnnotationFqNames && checker.checkAccessibility(type)
}

class KotlinTypeInaccessibleException(fqNames: Collection<FqName?>) : Exception() {
    override val message: String = "${StringUtil.pluralize(
        "Type",
        fqNames.size
    )} ${TypeAccessibilityChecker.typesToString(fqNames)} is not accessible from target module"
}

fun KtNamedDeclaration.isAlwaysActual(): Boolean = safeAs<KtParameter>()?.parent?.parent?.safeAs<KtPrimaryConstructor>()
    ?.mustHaveValOrVar() ?: false


fun TypeAccessibilityChecker.isCorrectAndHaveAccessibleModifiers(declaration: KtNamedDeclaration, showErrorHint: Boolean = false): Boolean {
    if (declaration.anyInaccessibleModifier(INACCESSIBLE_MODIFIERS, showErrorHint)) return false

    if (declaration is KtFunction && declaration.hasBody() && declaration.containingClassOrObject?.isInterfaceClass() == true) {
        if (showErrorHint) showInaccessibleDeclarationError(declaration, "The function declaration shouldn't have a default implementation")
        return false
    }

    if (!showErrorHint) return checkAccessibility(declaration)

    val types = incorrectTypes(declaration).ifEmpty { return true }
    showInaccessibleDeclarationError(
        declaration,
        "Some types are not accessible from ${targetModule.name}:\n" + TypeAccessibilityChecker.typesToString(
            types
        )
    )

    return false
}

private val INACCESSIBLE_MODIFIERS = listOf(KtTokens.PRIVATE_KEYWORD, KtTokens.CONST_KEYWORD, KtTokens.LATEINIT_KEYWORD)

private fun KtModifierListOwner.anyInaccessibleModifier(modifiers: Collection<KtModifierKeywordToken>, showErrorHint: Boolean): Boolean {
    for (modifier in modifiers) {
        if (hasModifier(modifier)) {
            if (showErrorHint) showInaccessibleDeclarationError(this, "The declaration has `$modifier` modifier")
            return true
        }
    }
    return false
}

fun showInaccessibleDeclarationError(element: PsiElement, message: String, editor: Editor? = element.findExistingEditor()) {
    editor?.let {
        showErrorHint(element.project, editor, escapeXml(message), "Inaccessible declaration")
    }
}

fun TypeAccessibilityChecker.Companion.typesToString(types: Collection<FqName?>, separator: CharSequence = "\n"): String {
    return types.toSet().joinToString(separator = separator) {
        it?.shortName()?.asString() ?: "<Unknown>"
    }
}

fun TypeAccessibilityChecker.findAndApplyExistingClasses(elements: Collection<KtNamedDeclaration>): HashSet<String> {
    var classes = elements.filterIsInstance<KtClassOrObject>()
    while (true) {
        val existingNames = classes.mapNotNull { it.fqName?.asString() }.toHashSet()
        existingTypeNames = existingNames

        val newExistingClasses = classes.filter { isCorrectAndHaveAccessibleModifiers(it) }
        if (classes.size == newExistingClasses.size) return existingNames

        classes = newExistingClasses
    }
}
