/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.quickfix.expectactual

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.ide.util.EditorHelper
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithContent
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.overrideImplement.OverrideMemberChooserObject.BodyType.EMPTY_OR_TEMPLATE
import org.jetbrains.kotlin.idea.core.overrideImplement.OverrideMemberChooserObject.BodyType.NO_BODY
import org.jetbrains.kotlin.idea.core.overrideImplement.OverrideMemberChooserObject.Companion.create
import org.jetbrains.kotlin.idea.core.overrideImplement.generateActualMember
import org.jetbrains.kotlin.idea.core.overrideImplement.generateTopLevelActual
import org.jetbrains.kotlin.idea.core.toDescriptor
import org.jetbrains.kotlin.idea.quickfix.KotlinQuickFixAction
import org.jetbrains.kotlin.idea.quickfix.KotlinSingleIntentionActionFactory
import org.jetbrains.kotlin.idea.util.actualsForExpected
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.MultiTargetPlatform
import org.jetbrains.kotlin.resolve.checkers.ExpectedActualDeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.ExperimentalUsageChecker
import org.jetbrains.kotlin.resolve.getMultiTargetPlatform

sealed class CreateActualFix<out D : KtNamedDeclaration>(
    declaration: D,
    private val actualModule: Module,
    private val actualPlatform: MultiTargetPlatform.Specific,
    private val generateIt: KtPsiFactory.(Project, D) -> D?
) : KotlinQuickFixAction<D>(declaration) {

    override fun getFamilyName() = text

    protected abstract val elementType: String

    override fun getText() = "Create actual $elementType for module ${actualModule.name} (${actualPlatform.platform})"

    override fun startInWriteAction() = false

    final override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val factory = KtPsiFactory(project)

        val actualFile = getOrCreateImplementationFile() ?: return
        DumbService.getInstance(project).runWhenSmart {
            val generated = factory.generateIt(project, element) ?: return@runWhenSmart

            project.executeWriteCommand("Create actual declaration") {
                if (actualFile.packageDirective?.fqName != file.packageDirective?.fqName &&
                    actualFile.declarations.isEmpty()
                ) {
                    val packageDirective = file.packageDirective
                    packageDirective?.let {
                        val oldPackageDirective = actualFile.packageDirective
                        val newPackageDirective = factory.createPackageDirective(it.fqName)
                        if (oldPackageDirective != null) {
                            oldPackageDirective.replace(newPackageDirective)
                        } else {
                            actualFile.add(newPackageDirective)
                        }
                    }
                }
                val actualDeclaration = actualFile.add(generated) as KtElement
                val reformatted = CodeStyleManager.getInstance(project).reformat(actualDeclaration)
                val shortened = ShortenReferences.DEFAULT.process(reformatted as KtElement)
                EditorHelper.openInEditor(shortened)?.caretModel?.moveToOffset(shortened.textRange.startOffset)
            }
        }
    }

    private fun getOrCreateImplementationFile(): KtFile? {
        val declaration = element as? KtNamedDeclaration ?: return null
        val parent = declaration.parent
        if (parent is KtFile) {
            for (otherDeclaration in parent.declarations) {
                if (otherDeclaration === declaration) continue
                if (!otherDeclaration.hasExpectModifier()) continue
                val actualDeclaration = otherDeclaration.actualsForExpected(actualModule).singleOrNull() ?: continue
                return actualDeclaration.containingKtFile
            }
        }
        return createFileForDeclaration(actualModule, declaration)
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val d = DiagnosticFactory.cast(diagnostic, Errors.NO_ACTUAL_FOR_EXPECT)
            val declaration = d.psiElement as? KtNamedDeclaration ?: return null
            val compatibility = d.c
            // For function we allow it, because overloads are possible
            if (compatibility.isNotEmpty() && declaration !is KtFunction) return null
            val actualModuleDescriptor = d.b
            val actualModule = (actualModuleDescriptor.getCapability(ModuleInfo.Capability) as? ModuleSourceInfo)?.module ?: return null
            val actualPlatform = actualModuleDescriptor.getMultiTargetPlatform() as? MultiTargetPlatform.Specific ?: return null
            return when (declaration) {
                is KtClassOrObject -> CreateActualClassFix(declaration, actualModule, actualPlatform)
                is KtFunction -> CreateActualFunctionFix(declaration, actualModule, actualPlatform)
                is KtProperty -> CreateActualPropertyFix(declaration, actualModule, actualPlatform)
                else -> null
            }
        }
    }
}

class CreateActualClassFix(
    klass: KtClassOrObject,
    actualModule: Module,
    actualPlatform: MultiTargetPlatform.Specific
) : CreateActualFix<KtClassOrObject>(klass, actualModule, actualPlatform, { project, element ->
    generateClassOrObjectByExpectedClass(project, element, actualNeeded = true)
}) {

    override val elementType = element.getTypeDescription()
}

class CreateActualPropertyFix(
    property: KtProperty,
    actualModule: Module,
    actualPlatform: MultiTargetPlatform.Specific
) : CreateActualFix<KtProperty>(property, actualModule, actualPlatform, { project, element ->
    val descriptor = element.toDescriptor() as? PropertyDescriptor
    descriptor?.let { generateProperty(project, element, descriptor) }
}) {

    override val elementType = "property"
}

class CreateActualFunctionFix(
    function: KtFunction,
    actualModule: Module,
    actualPlatform: MultiTargetPlatform.Specific
) : CreateActualFix<KtFunction>(function, actualModule, actualPlatform, { project, element ->
    val descriptor = element.toDescriptor() as? FunctionDescriptor
    descriptor?.let { generateFunction(project, element, descriptor) }
}) {

    override val elementType = "function"
}

private fun KtModifierListOwner.replaceExpectModifier(actualNeeded: Boolean) {
    if (actualNeeded) {
        addModifier(KtTokens.ACTUAL_KEYWORD)
    } else {
        removeModifier(KtTokens.HEADER_KEYWORD)
        removeModifier(KtTokens.EXPECT_KEYWORD)
    }
}

internal fun KtPsiFactory.generateClassOrObjectByExpectedClass(
    project: Project,
    expectedClass: KtClassOrObject,
    actualNeeded: Boolean,
    // If null, all expect class declarations are missed (so none from them exists)
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

    val actualClass = createClassCopyByText(expectedClass)
    actualClass.declarations.forEach {
        if (it.exists()) {
            it.delete()
            return@forEach
        }
        when (it) {
            is KtEnumEntry -> return@forEach
            is KtClassOrObject -> it.delete()
            is KtCallableDeclaration -> it.delete()
        }
    }
    actualClass.primaryConstructor?.delete()

    val context = expectedClass.analyzeWithContent()
    actualClass.superTypeListEntries.zip(expectedClass.superTypeListEntries).forEach { (actualEntry, expectedEntry) ->
        if (actualEntry !is KtSuperTypeEntry) return@forEach
        val superType = context[BindingContext.TYPE, expectedEntry.typeReference]
        val superClassDescriptor = superType?.constructor?.declarationDescriptor as? ClassDescriptor ?: return@forEach
        if (superClassDescriptor.kind == ClassKind.CLASS || superClassDescriptor.kind == ClassKind.ENUM_CLASS) {
            actualEntry.replace(createSuperTypeCallEntry("${actualEntry.typeReference!!.text}()"))
        }
    }
    if (actualClass.isAnnotation()) {
        actualClass.annotationEntries.zip(expectedClass.annotationEntries).forEach { (actualEntry, expectedEntry) ->
            val annotationDescriptor = context.get(BindingContext.ANNOTATION, expectedEntry) ?: return@forEach
            if (annotationDescriptor.fqName in forbiddenAnnotationFqNames) {
                actualEntry.delete()
            }
        }
    }
    actualClass.replaceExpectModifier(actualNeeded)

    declLoop@ for (expectedDeclaration in expectedClass.declarations.filter { !it.exists() }) {
        val descriptor = expectedDeclaration.toDescriptor() ?: continue
        val actualDeclaration: KtDeclaration = when (expectedDeclaration) {
            is KtClassOrObject ->
                if (expectedDeclaration !is KtEnumEntry) {
                    generateClassOrObjectByExpectedClass(project, expectedDeclaration, actualNeeded = true)
                } else {
                    continue@declLoop
                }
            is KtCallableDeclaration -> {
                when (expectedDeclaration) {
                    is KtFunction -> generateFunction(project, expectedDeclaration, descriptor as FunctionDescriptor, actualClass)
                    is KtProperty -> generateProperty(project, expectedDeclaration, descriptor as PropertyDescriptor, actualClass)
                    else -> continue@declLoop
                }
            }
            else -> continue@declLoop
        }
        actualClass.addDeclaration(actualDeclaration)
    }
    val expectedPrimaryConstructor = expectedClass.primaryConstructor
    if (actualClass is KtClass && expectedPrimaryConstructor?.exists() == false) {
        val descriptor = expectedPrimaryConstructor.toDescriptor()
        if (descriptor is FunctionDescriptor) {
            val actualPrimaryConstructor = generateFunction(project, expectedPrimaryConstructor, descriptor, actualClass)
            actualClass.createPrimaryConstructorIfAbsent().replace(actualPrimaryConstructor)
        }
    }

    return actualClass
}

private val forbiddenAnnotationFqNames = setOf(
    ExpectedActualDeclarationChecker.OPTIONAL_EXPECTATION_FQ_NAME,
    FqName("kotlin.ExperimentalMultiplatform"),
    ExperimentalUsageChecker.USE_EXPERIMENTAL_FQ_NAME
)

private fun generateFunction(
    project: Project,
    expectedFunction: KtFunction,
    descriptor: FunctionDescriptor,
    targetClass: KtClassOrObject? = null
): KtFunction {
    val memberChooserObject = create(
        expectedFunction, descriptor, descriptor,
        if (descriptor.modality == Modality.ABSTRACT) NO_BODY else EMPTY_OR_TEMPLATE
    )
    return if (targetClass != null) {
        memberChooserObject.generateActualMember(targetClass = targetClass, copyDoc = true)
    } else {
        memberChooserObject.generateTopLevelActual(project = project, copyDoc = true)
    } as KtFunction
}

private fun generateProperty(
    project: Project,
    expectedProperty: KtProperty,
    descriptor: PropertyDescriptor,
    targetClass: KtClassOrObject? = null
): KtProperty {
    val memberChooserObject = create(
        expectedProperty, descriptor, descriptor,
        if (descriptor.modality == Modality.ABSTRACT) NO_BODY else EMPTY_OR_TEMPLATE
    )
    return if (targetClass != null) {
        memberChooserObject.generateActualMember(targetClass = targetClass, copyDoc = true)
    } else {
        memberChooserObject.generateTopLevelActual(project = project, copyDoc = true)
    } as KtProperty
}

