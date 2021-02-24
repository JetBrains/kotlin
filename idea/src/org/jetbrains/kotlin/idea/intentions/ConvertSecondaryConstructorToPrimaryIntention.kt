/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithContent
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.idea.intentions.ConvertSecondaryConstructorToPrimaryIntention.Companion.tryConvertToPropertyByParameterInitialization
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.util.kind

class ConvertSecondaryConstructorToPrimaryInspection : IntentionBasedInspection<KtSecondaryConstructor>(
    ConvertSecondaryConstructorToPrimaryIntention::class,
    fun(constructor: KtSecondaryConstructor): Boolean {
        val containingClass = constructor.containingClass() ?: return false
        if (containingClass.secondaryConstructors.size != 1) return false
        val context = containingClass.analyzeWithContent()
        val constructorDescriptor = context[BindingContext.CONSTRUCTOR, constructor] ?: return false
        for (statement in constructor.bodyExpression?.statements.orEmpty()) {
            val propertyDescriptor = statement.tryConvertToPropertyByParameterInitialization(constructorDescriptor, context)?.second
            val property = propertyDescriptor?.let { DescriptorToSourceUtils.descriptorToDeclaration(it) as? KtProperty }
            if (property?.setter?.hasBody() == true) return false
        }
        return true
    }
) {
    override fun inspectionTarget(element: KtSecondaryConstructor) = element.getConstructorKeyword()
}

class ConvertSecondaryConstructorToPrimaryIntention : SelfTargetingRangeIntention<KtSecondaryConstructor>(
    KtSecondaryConstructor::class.java,
    KotlinBundle.lazyMessage("convert.to.primary.constructor")
) {
    private tailrec fun ConstructorDescriptor.isReachableByDelegationFrom(
        constructor: ConstructorDescriptor, context: BindingContext, visited: Set<ConstructorDescriptor> = emptySet()
    ): Boolean {
        if (constructor == this) return true
        if (constructor in visited) return false
        val resolvedDelegationCall = context[BindingContext.CONSTRUCTOR_RESOLVED_DELEGATION_CALL, constructor] ?: return false
        val delegationDescriptor = resolvedDelegationCall.candidateDescriptor
        return isReachableByDelegationFrom(delegationDescriptor, context, visited + constructor)
    }

    override fun applicabilityRange(element: KtSecondaryConstructor): TextRange? {
        val delegationCall = element.getDelegationCall()
        if (delegationCall.isCallToThis) return null
        val klass = element.containingClassOrObject ?: return null
        if (klass.hasPrimaryConstructor()) return null

        val context = klass.analyzeWithContent()
        val classDescriptor = context[BindingContext.CLASS, klass] ?: return null
        val elementDescriptor = context[BindingContext.CONSTRUCTOR, element] ?: return null

        for (constructorDescriptor in classDescriptor.constructors) {
            if (constructorDescriptor == elementDescriptor) continue
            if (!elementDescriptor.isReachableByDelegationFrom(constructorDescriptor, context)) return null
        }

        return TextRange(element.startOffset, element.valueParameterList?.endOffset ?: element.getConstructorKeyword().endOffset)
    }

    private fun KtSecondaryConstructor.extractInitializer(
        parameterToPropertyMap: MutableMap<ValueParameterDescriptor, PropertyDescriptor>,
        context: BindingContext,
        factory: KtPsiFactory
    ): KtClassInitializer? {
        val constructorDescriptor = context[BindingContext.CONSTRUCTOR, this] ?: return null
        val initializer = factory.createAnonymousInitializer() as? KtClassInitializer
        for (statement in bodyExpression?.statements ?: emptyList()) {
            val (rightDescriptor, leftDescriptor) =
                statement.tryConvertToPropertyByParameterInitialization(constructorDescriptor, context) ?: with(initializer) {
                    (initializer?.body as? KtBlockExpression)?.let {
                        it.addBefore(statement.copy(), it.rBrace)
                        it.addBefore(factory.createNewLine(), it.rBrace)
                    }
                    null to null
                }

            if (rightDescriptor == null || leftDescriptor == null) continue
            parameterToPropertyMap[rightDescriptor] = leftDescriptor
        }
        return initializer
    }

    private fun KtSecondaryConstructor.moveParametersToPrimaryConstructorAndInitializers(
        primaryConstructor: KtPrimaryConstructor,
        parameterToPropertyMap: MutableMap<ValueParameterDescriptor, PropertyDescriptor>,
        context: BindingContext,
        factory: KtPsiFactory
    ) {
        val parameterList = primaryConstructor.valueParameterList!!
        for (parameter in valueParameters) {
            val newParameter = factory.createParameter(parameter.text)
            val parameterDescriptor = context[BindingContext.VALUE_PARAMETER, parameter]
            val propertyDescriptor = parameterToPropertyMap[parameterDescriptor]
            var propertyCommentSaver: CommentSaver? = null
            if (parameterDescriptor != null && propertyDescriptor != null) {
                val property = DescriptorToSourceUtils.descriptorToDeclaration(propertyDescriptor) as? KtProperty
                if (property != null) {
                    if (propertyDescriptor.name == parameterDescriptor.name &&
                        propertyDescriptor.type == parameterDescriptor.type &&
                        propertyDescriptor.accessors.all { it.isDefault }
                    ) {
                        propertyCommentSaver = CommentSaver(property)
                        val valOrVar = if (property.isVar) factory.createVarKeyword() else factory.createValKeyword()
                        newParameter.addBefore(valOrVar, newParameter.nameIdentifier)
                        val propertyModifiers = property.modifierList?.text
                        if (propertyModifiers != null) {
                            val newModifiers = factory.createModifierList(propertyModifiers)
                            newParameter.addBefore(newModifiers, newParameter.valOrVarKeyword)
                        }
                        property.delete()
                    } else {
                        property.initializer = factory.createSimpleName(parameterDescriptor.name.asString())
                    }
                }
            }
            with(parameterList.addParameter(newParameter)) {
                propertyCommentSaver?.restore(this@with)
            }
        }
    }

    override fun applyTo(element: KtSecondaryConstructor, editor: Editor?) {
        val klass = element.containingClassOrObject as? KtClass ?: return
        val context = klass.analyzeWithContent()
        val factory = KtPsiFactory(klass)
        val constructorCommentSaver = CommentSaver(element)
        val constructorInClass = klass.createPrimaryConstructorIfAbsent()
        val constructor = factory.createPrimaryConstructorWithModifiers(element.modifierList?.text?.replace("\n", " "))

        val parameterToPropertyMap = mutableMapOf<ValueParameterDescriptor, PropertyDescriptor>()
        val initializer = element.extractInitializer(parameterToPropertyMap, context, factory) ?: return

        element.moveParametersToPrimaryConstructorAndInitializers(constructor, parameterToPropertyMap, context, factory)

        val argumentList = element.getDelegationCall().valueArgumentList
        for (superTypeListEntry in klass.superTypeListEntries) {
            val typeReference = superTypeListEntry.typeReference ?: continue
            val type = context[BindingContext.TYPE, typeReference]
            if ((type?.constructor?.declarationDescriptor as? ClassifierDescriptorWithTypeParameters)?.kind == ClassKind.CLASS) {
                val superTypeCallEntry = factory.createSuperTypeCallEntry(
                    "${typeReference.text}${argumentList?.text ?: "()"}"
                )
                superTypeListEntry.replace(superTypeCallEntry)
                break
            }
        }

        with(constructorInClass.replace(constructor)) {
            constructorCommentSaver.restore(this)
        }

        if ((initializer.body as? KtBlockExpression)?.statements?.isNotEmpty() == true) {
            val hasPropertyAfterInitializer = element.parent.children.takeLastWhile { it !== element }.any {
                it is KtProperty && ReferencesSearch.search(it, initializer.useScope).findFirst() != null
            }
            if (hasPropertyAfterInitializer) {
                // In this case we must move init {} down, because it uses a property declared below
                klass.addDeclaration(initializer)
                element.delete()
            } else {
                element.replace(initializer)
            }
        } else {
            element.delete()
        }
    }

    companion object {
        fun KtExpression.tryConvertToPropertyByParameterInitialization(
            constructorDescriptor: ConstructorDescriptor, context: BindingContext
        ): Pair<ValueParameterDescriptor, PropertyDescriptor>? {
            if (this !is KtBinaryExpression || operationToken != KtTokens.EQ) return null
            val rightReference = right as? KtReferenceExpression ?: return null
            val rightDescriptor = context[BindingContext.REFERENCE_TARGET, rightReference] as? ValueParameterDescriptor ?: return null
            if (rightDescriptor.containingDeclaration != constructorDescriptor) return null
            val leftReference = when (val left = left) {
                is KtReferenceExpression ->
                    left
                is KtDotQualifiedExpression ->
                    if (left.receiverExpression is KtThisExpression) left.selectorExpression as? KtReferenceExpression else null
                else ->
                    null
            }
            val leftDescriptor = context[BindingContext.REFERENCE_TARGET, leftReference] as? PropertyDescriptor ?: return null
            return rightDescriptor to leftDescriptor
        }
    }
}