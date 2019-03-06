/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.expectactual

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaDirectoryService
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.project.implementedDescriptors
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithContent
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.findOrCreateDirectoryForPackage
import org.jetbrains.kotlin.idea.core.getPackage
import org.jetbrains.kotlin.idea.core.overrideImplement.*
import org.jetbrains.kotlin.idea.core.overrideImplement.OverrideMemberChooserObject.BodyType.EMPTY_OR_TEMPLATE
import org.jetbrains.kotlin.idea.core.overrideImplement.OverrideMemberChooserObject.BodyType.NO_BODY
import org.jetbrains.kotlin.idea.core.overrideImplement.OverrideMemberChooserObject.Companion.create
import org.jetbrains.kotlin.idea.core.toDescriptor
import org.jetbrains.kotlin.idea.project.platform
import org.jetbrains.kotlin.idea.refactoring.createKotlinFile
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.idea.util.hasDeclarationOf
import org.jetbrains.kotlin.idea.util.isEffectivelyActual
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.impl.isCommon
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.hasActualModifier
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.checkers.ExpectedActualDeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.ExperimentalUsageChecker
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.isCommon
import org.jetbrains.kotlin.types.AbbreviatedType
import org.jetbrains.kotlin.types.KotlinType

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
            if (packageDirective?.packageNameExpression == null) directory.getPackage()?.qualifiedName
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
    outerClasses: List<KtClassOrObject> = emptyList(),
    // If null, all class declarations are missed (so none from them exists)
    missedDeclarations: List<KtDeclaration>? = null
): KtClassOrObject {
    fun areCompatible(first: KtFunction, second: KtFunction) =
        first.valueParameters.size == second.valueParameters.size &&
                first.valueParameters.zip(second.valueParameters).all { (firstParam, secondParam) ->
                    firstParam.name == secondParam.name && firstParam.typeReference?.text == secondParam.typeReference?.text
                }

    fun KtDeclaration.exists() =
        missedDeclarations != null && missedDeclarations.none {
            name == it.name && when (this) {
                is KtConstructor<*> -> it is KtConstructor<*> && areCompatible(this, it)
                is KtNamedFunction -> it is KtNamedFunction && areCompatible(this, it)
                is KtProperty -> it is KtProperty || it is KtParameter && it.hasValOrVar()
                else -> this.javaClass == it.javaClass
            }
        }

    val generatedClass = createClassHeaderCopyByText(originalClass)
    val context = originalClass.analyzeWithContent()

    generatedClass.superTypeListEntries.zip(originalClass.superTypeListEntries).forEach { (generatedEntry, originalEntry) ->
        if (generateExpectClass) {
            if (generatedEntry !is KtSuperTypeCallEntry) return@forEach
        } else {
            if (generatedEntry !is KtSuperTypeEntry) return@forEach
        }
        val superType = context[BindingContext.TYPE, originalEntry.typeReference]
        val superClassDescriptor = superType?.constructor?.declarationDescriptor as? ClassDescriptor ?: return@forEach
        if (superClassDescriptor.kind == ClassKind.CLASS || superClassDescriptor.kind == ClassKind.ENUM_CLASS) {
            val newGeneratedEntry = if (generateExpectClass) {
                createSuperTypeEntry(generatedEntry.typeReference!!.text)
            } else {
                createSuperTypeCallEntry(generatedEntry.typeReference!!.text + "()")
            }
            generatedEntry.replace(newGeneratedEntry)
        }
    }
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
    declLoop@ for (originalDeclaration in originalClass.declarations.filter { !it.exists() }) {
        val descriptor = originalDeclaration.toDescriptor() ?: continue
        if (generateExpectClass && !originalDeclaration.isEffectivelyActual()) continue
        val generatedDeclaration: KtDeclaration = when (originalDeclaration) {
            is KtClassOrObject -> {
                generateClassOrObject(project, generateExpectClass, originalDeclaration, outerClasses + generatedClass)
            }
            is KtCallableDeclaration -> {
                when (originalDeclaration) {
                    is KtFunction -> generateFunction(
                        project, generateExpectClass, originalDeclaration, descriptor as FunctionDescriptor, generatedClass, outerClasses
                    )
                    is KtProperty -> generateProperty(
                        project, generateExpectClass, originalDeclaration, descriptor as PropertyDescriptor, generatedClass, outerClasses
                    )
                    else -> continue@declLoop
                }
            }
            else -> continue@declLoop
        }
        generatedClass.addDeclaration(generatedDeclaration)
    }
    if (!originalClass.isAnnotation()) {
        for (originalProperty in originalClass.primaryConstructorParameters) {
            if (!originalProperty.hasValOrVar() || !originalProperty.hasActualModifier()) continue
            val descriptor = originalProperty.toDescriptor() as? PropertyDescriptor ?: continue
            val generatedProperty = generateProperty(
                project, generateExpectClass, originalProperty, descriptor, generatedClass, outerClasses
            )
            generatedClass.addDeclaration(generatedProperty)
        }
    }
    val originalPrimaryConstructor = originalClass.primaryConstructor
    if (generatedClass is KtClass && originalPrimaryConstructor != null && !originalPrimaryConstructor.exists()) {
        val descriptor = originalPrimaryConstructor.toDescriptor()
        if (descriptor is FunctionDescriptor) {
            val expectedPrimaryConstructor = generateFunction(
                project, generateExpectClass, originalPrimaryConstructor, descriptor, generatedClass, outerClasses
            )
            generatedClass.createPrimaryConstructorIfAbsent().replace(expectedPrimaryConstructor)
        }
    }

    return generatedClass
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
    outerClasses: List<KtClassOrObject> = emptyList()
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
    ) as KtFunction
}

internal fun generateProperty(
    project: Project,
    generateExpect: Boolean,
    originalProperty: KtNamedDeclaration,
    descriptor: PropertyDescriptor,
    generatedClass: KtClassOrObject? = null,
    outerClasses: List<KtClassOrObject> = emptyList()
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
    ) as KtProperty
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
    if (declaration?.module?.platform?.kind?.isCommon == true) {
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
