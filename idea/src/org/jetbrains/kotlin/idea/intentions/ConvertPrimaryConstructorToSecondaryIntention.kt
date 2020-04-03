/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtTokens.THIS_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.VARARG_KEYWORD
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.KtPsiFactory.CallableBuilder
import org.jetbrains.kotlin.psi.KtPsiFactory.CallableBuilder.Target.CONSTRUCTOR
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.parents

class ConvertPrimaryConstructorToSecondaryIntention : SelfTargetingIntention<KtPrimaryConstructor>(
    KtPrimaryConstructor::class.java,
    KotlinBundle.lazyMessage("convert.to.secondary.constructor")
) {
    override fun isApplicableTo(element: KtPrimaryConstructor, caretOffset: Int): Boolean {
        val containingClass = element.containingClassOrObject as? KtClass ?: return false
        if (containingClass.isAnnotation() || containingClass.isData()
            || containingClass.superTypeListEntries.any { it is KtDelegatedSuperTypeEntry }
        ) return false
        return element.valueParameters.all { !it.hasValOrVar() || (it.name != null && it.annotationEntries.isEmpty()) }
    }

    private fun KtReferenceExpression.isIndependent(classDescriptor: ClassDescriptor, context: BindingContext): Boolean =
        when (val referencedDescriptor = context[BindingContext.REFERENCE_TARGET, this]) {
            null ->
                false
            is ValueParameterDescriptor ->
                (referencedDescriptor.containingDeclaration as? ConstructorDescriptor)?.containingDeclaration != classDescriptor
            else ->
                classDescriptor !in referencedDescriptor.parents
        }

    private fun KtProperty.isIndependent(klass: KtClass, context: BindingContext): Boolean {
        val propertyInitializer = initializer ?: return true
        val classDescriptor = context[BindingContext.CLASS, klass] ?: return false
        return !propertyInitializer.anyDescendantOfType<KtReferenceExpression> { !it.isIndependent(classDescriptor, context) }
    }

    override fun applyTo(element: KtPrimaryConstructor, editor: Editor?) {
        val klass = element.containingClassOrObject as? KtClass ?: return
        if (klass.isAnnotation()) return
        val context = klass.analyze()
        val factory = KtPsiFactory(klass)
        val commentSaver = CommentSaver(element)
        val initializerMap = mutableMapOf<KtProperty, String>()
        for (property in klass.getProperties()) {
            if (property.isIndependent(klass, context)) continue
            if (property.typeReference == null) {
                with(SpecifyTypeExplicitlyIntention()) {
                    if (applicabilityRange(property) != null) {
                        applyTo(property, editor)
                    }
                }
            }
            val initializer = property.initializer!!
            initializerMap[property] = initializer.text
            initializer.delete()
            property.equalsToken!!.delete()
        }
        val constructor = factory.createSecondaryConstructor(
            CallableBuilder(CONSTRUCTOR).apply {
                element.modifierList?.let { modifier(it.text) }
                typeParams()
                name()
                for (valueParameter in element.valueParameters) {
                    val annotations = valueParameter.annotationEntries.joinToString(separator = " ") { it.text }
                    val vararg = if (valueParameter.isVarArg) VARARG_KEYWORD.value else ""
                    param(
                        "$annotations $vararg ${valueParameter.name ?: ""}",
                        valueParameter.typeReference?.text ?: "", valueParameter.defaultValue?.text
                    )
                }
                noReturnType()
                for (superTypeEntry in klass.superTypeListEntries) {
                    if (superTypeEntry is KtSuperTypeCallEntry) {
                        superDelegation(superTypeEntry.valueArgumentList?.text ?: "")
                        superTypeEntry.replace(factory.createSuperTypeEntry(superTypeEntry.typeReference!!.text))
                    }
                }
                val valueParameterInitializers =
                    element.valueParameters.asSequence().filter { it.hasValOrVar() }.joinToString(separator = "\n") {
                        val name = it.name!!
                        "this.$name = $name"
                    }
                val classBodyInitializers = klass.declarations.asSequence().filter {
                    (it is KtProperty && initializerMap[it] != null) || it is KtAnonymousInitializer
                }.joinToString(separator = "\n") {
                    if (it is KtProperty) {
                        val name = it.name!!
                        val text = initializerMap[it]
                        if (text != null) {
                            "${THIS_KEYWORD.value}.$name = $text"
                        } else {
                            ""
                        }
                    } else {
                        ((it as KtAnonymousInitializer).body as? KtBlockExpression)?.statements?.joinToString(separator = "\n") {
                            it.text
                        } ?: ""
                    }
                }
                val allInitializers = listOf(valueParameterInitializers, classBodyInitializers).filter(String::isNotEmpty)
                if (allInitializers.isNotEmpty()) {
                    blockBody(allInitializers.joinToString(separator = "\n"))
                }
            }.asString()
        )

        val lastEnumEntry = klass.declarations.lastOrNull { it is KtEnumEntry } as? KtEnumEntry
        val secondaryConstructor =
            lastEnumEntry?.let { klass.addDeclarationAfter(constructor, it) } ?: klass.addDeclarationBefore(constructor, null)
        commentSaver.restore(secondaryConstructor)

        convertValueParametersToProperties(element, klass, factory, lastEnumEntry)
        if (klass.isEnum()) {
            addSemicolonIfNotExist(klass, factory, lastEnumEntry)
        }

        for (anonymousInitializer in klass.getAnonymousInitializers()) {
            anonymousInitializer.delete()
        }
        element.delete()
    }

    private fun convertValueParametersToProperties(
        element: KtPrimaryConstructor, klass: KtClass, factory: KtPsiFactory, anchorBefore: PsiElement?
    ) {
        for (valueParameter in element.valueParameters.reversed()) {
            if (!valueParameter.hasValOrVar()) continue
            val isVararg = valueParameter.hasModifier(VARARG_KEYWORD)
            valueParameter.removeModifier(VARARG_KEYWORD)
            val typeText = valueParameter.typeReference?.text
            val property = factory.createProperty(
                valueParameter.modifierList?.text, valueParameter.name!!,
                if (isVararg && typeText != null) "Array<out $typeText>" else typeText,
                valueParameter.isMutable, null
            )
            if (anchorBefore == null) klass.addDeclarationBefore(property, null) else klass.addDeclarationAfter(property, anchorBefore)
        }
    }

    private fun addSemicolonIfNotExist(klass: KtClass, factory: KtPsiFactory, lastEnumEntry: KtEnumEntry?) {
        if (lastEnumEntry == null) {
            klass.getOrCreateBody().let { it.addAfter(factory.createSemicolon(), it.lBrace) }
        } else if (lastEnumEntry.getChildrenOfType<LeafPsiElement>().none { it.elementType == KtTokens.SEMICOLON }) {
            lastEnumEntry.add(factory.createSemicolon())
        }
    }
}