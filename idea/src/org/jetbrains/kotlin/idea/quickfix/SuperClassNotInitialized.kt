/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.hint.ShowParameterInfoHandler
import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.caches.resolve.resolveToParameterDescriptorIfAny
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.isVisible
import org.jetbrains.kotlin.idea.core.moveCaret
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeConstructorSubstitution
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*

object SuperClassNotInitialized : KotlinIntentionActionsFactory() {
    private const val DISPLAY_MAX_PARAMS = 5

    override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
        val delegator = diagnostic.psiElement as KtSuperTypeEntry
        val classOrObjectDeclaration = delegator.parent.parent as? KtClassOrObject ?: return emptyList()

        val typeRef = delegator.typeReference ?: return emptyList()
        val type = typeRef.analyze()[BindingContext.TYPE, typeRef] ?: return emptyList()
        if (type.isError) return emptyList()

        val superClass = (type.constructor.declarationDescriptor as? ClassDescriptor) ?: return emptyList()
        val classDescriptor = classOrObjectDeclaration.resolveToDescriptorIfAny(BodyResolveMode.FULL) ?: return emptyList()
        val containingPackage = superClass.classId?.packageFqName
        val inSamePackage = containingPackage != null && containingPackage == classDescriptor.classId?.packageFqName
        val constructors = superClass.constructors.filter {
            it.isVisible(classDescriptor) && (superClass.modality != Modality.SEALED || inSamePackage && classDescriptor.visibility != DescriptorVisibilities.LOCAL)
        }
        if (constructors.isEmpty() && (!superClass.isExpect || superClass.kind != ClassKind.CLASS)) {
            return emptyList() // no accessible constructor
        }

        val fixes = ArrayList<IntentionAction>()

        fixes.add(
            AddParenthesisFix(
                delegator,
                putCaretIntoParenthesis = constructors.singleOrNull()?.valueParameters?.isNotEmpty() ?: true
            )
        )

        if (classOrObjectDeclaration is KtClass) {
            val superType = classDescriptor.typeConstructor.supertypes.firstOrNull { it.constructor.declarationDescriptor == superClass }
            if (superType != null) {
                val substitutor = TypeConstructorSubstitution.create(superClass.typeConstructor, superType.arguments).buildSubstitutor()

                val substitutedConstructors = constructors
                    .asSequence()
                    .filter { it.valueParameters.isNotEmpty() }
                    .mapNotNull { it.substitute(substitutor) }
                    .toList()

                if (substitutedConstructors.isNotEmpty()) {
                    val parameterTypes: List<List<KotlinType>> = substitutedConstructors.map {
                        it.valueParameters.map { it.type }
                    }

                    fun canRenderOnlyFirstParameters(n: Int) = parameterTypes.map { it.take(n) }.toSet().size == parameterTypes.size

                    val maxParams = parameterTypes.maxOf { it.size }
                    val maxParamsToDisplay = if (maxParams <= DISPLAY_MAX_PARAMS) {
                        maxParams
                    } else {
                        (DISPLAY_MAX_PARAMS until maxParams).firstOrNull(::canRenderOnlyFirstParameters) ?: maxParams
                    }

                    for ((constructor, types) in substitutedConstructors.zip(parameterTypes)) {
                        val typesRendered =
                            types.asSequence().take(maxParamsToDisplay).map { DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(it) }
                                .toList()
                        val parameterString = typesRendered.joinToString(", ", "(", if (types.size <= maxParamsToDisplay) ")" else ",...)")
                        val text = KotlinBundle.message("add.constructor.parameters.from.0.1", superClass.name.asString(), parameterString)
                        fixes.addIfNotNull(AddParametersFix.create(delegator, classOrObjectDeclaration, constructor, text))
                    }
                }
            }
        }

        return fixes
    }

    private class AddParenthesisFix(
        element: KtSuperTypeEntry,
        val putCaretIntoParenthesis: Boolean
    ) : KotlinQuickFixAction<KtSuperTypeEntry>(element), HighPriorityAction {

        override fun getFamilyName() = KotlinBundle.message("change.to.constructor.invocation") //TODO?

        override fun getText() = familyName

        override fun invoke(project: Project, editor: Editor?, file: KtFile) {
            val element = element ?: return
            val context = (element.getStrictParentOfType<KtClassOrObject>() ?: element).analyze()
            val baseClass = AddDefaultConstructorFix.superTypeEntryToClass(element, context)

            val newSpecifier = element.replaced(KtPsiFactory(project).createSuperTypeCallEntry(element.text + "()"))
            if (baseClass != null && baseClass.hasExpectModifier() && baseClass.secondaryConstructors.isEmpty()) {
                baseClass.createPrimaryConstructorIfAbsent()
            }

            if (putCaretIntoParenthesis) {
                if (editor != null) {
                    val offset = newSpecifier.valueArgumentList!!.leftParenthesis!!.endOffset
                    editor.moveCaret(offset)
                    if (!ApplicationManager.getApplication().isUnitTestMode) {
                        ShowParameterInfoHandler.invoke(project, editor, file, offset - 1, null, true)
                    }
                }
            }
        }
    }

    private class AddParametersFix(
        element: KtSuperTypeEntry,
        classDeclaration: KtClass,
        parametersToAdd: Collection<KtParameter>,
        private val argumentText: String,
        private val text: String
    ) : KotlinQuickFixAction<KtSuperTypeEntry>(element) {
        private val classDeclarationPointer = classDeclaration.createSmartPointer()
        private val parametersToAddPointers = parametersToAdd.map { it.createSmartPointer() }

        companion object {
            fun create(
                element: KtSuperTypeEntry,
                classDeclaration: KtClass,
                superConstructor: ConstructorDescriptor,
                text: String
            ): AddParametersFix? {
                val superParameters = superConstructor.valueParameters
                assert(superParameters.isNotEmpty())

                if (superParameters.any { it.type.isError }) return null

                val argumentText = StringBuilder()
                val oldParameters = classDeclaration.primaryConstructorParameters
                val parametersToAdd = ArrayList<KtParameter>()
                for (parameter in superParameters) {
                    val nameRendered = parameter.name.render()
                    val varargElementType = parameter.varargElementType

                    if (argumentText.isNotEmpty()) {
                        argumentText.append(", ")
                    }
                    argumentText.append(if (varargElementType != null) "*$nameRendered" else nameRendered)

                    val nameString = parameter.name.asString()
                    val existingParameter = oldParameters.firstOrNull { it.name == nameString }
                    if (existingParameter != null) {
                        val type = (existingParameter.resolveToParameterDescriptorIfAny() as? VariableDescriptor)?.type
                            ?: return null
                        if (type.isSubtypeOf(parameter.type)) continue // use existing parameter
                    }

                    val defaultValue = if (parameter.declaresDefaultValue()) {
                        (DescriptorToSourceUtils.descriptorToDeclaration(parameter) as? KtParameter)
                            ?.defaultValue?.text?.let { " = $it" } ?: ""
                    } else {
                        ""
                    }
                    val parameterText = if (varargElementType != null) {
                        "vararg " + nameRendered + ":" + IdeDescriptorRenderers.SOURCE_CODE.renderType(varargElementType)
                    } else {
                        nameRendered + ":" + IdeDescriptorRenderers.SOURCE_CODE.renderType(parameter.type)
                    } + defaultValue
                    parametersToAdd.add(KtPsiFactory(element).createParameter(parameterText))
                }

                return AddParametersFix(element, classDeclaration, parametersToAdd, argumentText.toString(), text)
            }
        }

        override fun getFamilyName() = KotlinBundle.message("add.constructor.parameters.from.superclass")

        override fun getText() = text

        override fun invoke(project: Project, editor: Editor?, file: KtFile) {
            val element = element ?: return
            val classDeclaration = classDeclarationPointer.element ?: return
            val parametersToAdd = parametersToAddPointers.map { it.element ?: return }
            val factory = KtPsiFactory(project)

            val typeRefsToShorten = ArrayList<KtTypeReference>()
            val parameterList = classDeclaration.createPrimaryConstructorParameterListIfAbsent()

            for (parameter in parametersToAdd) {
                val newParameter = parameterList.addParameter(parameter)
                typeRefsToShorten.add(newParameter.typeReference!!)
            }

            val delegatorCall = factory.createSuperTypeCallEntry(element.text + "(" + argumentText + ")")
            element.replace(delegatorCall)

            ShortenReferences.DEFAULT.process(typeRefsToShorten)
        }
    }
}
