/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compiler.visualizer

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.compiler.visualizer.Annotator.annotate
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.renderer.ClassifierNamePolicy
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.ParameterNameRenderingPolicy
import org.jetbrains.kotlin.resolve.BindingContext.*
import org.jetbrains.kotlin.resolve.bindingContextUtil.getAbbreviatedTypeOrType
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.types.*

class PsiRenderer(private val file: KtFile, analysisResult: AnalysisResult) : BaseRenderer {
    val bindingContext = analysisResult.bindingContext
    private val annotations = mutableListOf<Annotator.AnnotationInfo>()

    val descriptorRenderer: DescriptorRenderer = DescriptorRenderer.withOptions {
        withDefinedIn = false
        modifiers = emptySet()
        receiverAfterName = false
        startFromName = false
        withSourceFileForTopLevel = false
        renderConstructorKeyword = false
        classifierNamePolicy = ClassifierNamePolicy.SOURCE_CODE_QUALIFIED
        includeAdditionalModifiers = false
        renderContextNearLocalVariable = true
        fullContextForLocalVariable = false
        useBaseClassAsReceiver = true
        parameterNameRenderingPolicy = ParameterNameRenderingPolicy.NONE
    }

    private val unnecessaryData = mapOf(
        "kotlin." to "",
        "collections." to ""
    )

    private fun addAnnotation(annotationText: String, element: PsiElement) {
        annotations.removeIf { it.range.startOffset == element.textRange.startOffset }

        var textWithOutUnnecessaryData = annotationText
        for ((key, value) in unnecessaryData) {
            textWithOutUnnecessaryData = textWithOutUnnecessaryData.replace(key, value)
        }
        if (textWithOutUnnecessaryData != element.text && textWithOutUnnecessaryData.isNotEmpty()) {
            annotations.add(Annotator.AnnotationInfo(textWithOutUnnecessaryData, element.textRange))
        }
    }

    override fun render(): String {
        file.accept(Renderer())
        return annotate(file.text, annotations).joinToString("\n")
    }

    inner class Renderer : KtVisitorVoid() {
        private fun renderType(type: KotlinType?): String {
            return type?.let { descriptorRenderer.renderType(it) } ?: "[ERROR: unknown type]"
        }

        private fun renderType(descriptor: CallableDescriptor?): String {
            return renderType(descriptor?.returnType)
        }

        private fun renderType(expression: KtExpression?): String {
            return renderType(expression?.let { bindingContext.getType(it) })
        }

        override fun visitElement(element: PsiElement) {
            element.acceptChildren(this)
        }

        override fun visitKtElement(element: KtElement) {
            element.acceptChildren(this)
        }

        override fun visitPackageDirective(directive: KtPackageDirective) {
            //don't resolve package names
        }

        override fun visitSuperExpression(expression: KtSuperExpression) {
            //don't resolve super expression
        }

        override fun visitThisExpression(expression: KtThisExpression) {
            //don't resolve this expression
        }

        private fun renderVariableType(variable: KtVariableDeclaration) {
            val descriptor = bindingContext[VARIABLE, variable]
            addAnnotation(renderType(descriptor), variable.nameIdentifier!!)
            variable.acceptChildren(this)
        }

        override fun visitProperty(property: KtProperty) =
            renderVariableType(property)

        override fun visitDestructuringDeclarationEntry(multiDeclarationEntry: KtDestructuringDeclarationEntry) =
            renderVariableType(multiDeclarationEntry)

        override fun visitTypeReference(typeReference: KtTypeReference) {
            val type = typeReference.getAbbreviatedTypeOrType(bindingContext)
            addAnnotation(renderType(type), typeReference)
            super.visitTypeReference(typeReference)
        }

        override fun visitConstantExpression(expression: KtConstantExpression) {
            addAnnotation(renderType(expression), expression)
        }

        private fun renderCall(call: Call): String {
            val resolvedCall = bindingContext[RESOLVED_CALL, call] ?: return "[ERROR: not resolved]"

            val descriptor = resolvedCall.resultingDescriptor
            var annotation =
                if (descriptor.isSpecial()) descriptorRenderer.renderSpecialFunction(descriptor)
                else descriptorRenderer.render(descriptor)
            if (descriptor is ClassConstructorDescriptor) annotation = annotation.replace("(", "<init>(")

            resolvedCall.typeArguments.forEach { (type, value) ->
                val newValue = if (value is ErrorType) "???" else "$value"
                annotation = annotation.replaceFirst(type.name.toString(), newValue)
            }
            return annotation
        }

        private fun CallableDescriptor.isSpecial(): Boolean {
            return this.name.asString().contains("SPECIAL-FUNCTION")
        }

        private fun DescriptorRenderer.renderSpecialFunction(descriptor: CallableDescriptor): String {
            val descriptorName = descriptor.name.asString()
            val name = when {
                descriptorName.contains("ELVIS") -> "?:"
                descriptorName.contains("EXCLEXCL") -> "!!"
                else -> "UNKNOWN"
            }
            return "fun $name " + this.renderValueParameters(descriptor.valueParameters, false) +
                    ": " + renderType(descriptor)
        }

        override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
            val call = expression.getCall(bindingContext)
            if (call != null) {
                addAnnotation(renderCall(call), expression)
                super.visitSimpleNameExpression(expression)
            } else {
                val qualifierDescriptor = bindingContext[QUALIFIER, expression]?.descriptor
                if (qualifierDescriptor != null) {
                    addAnnotation(descriptorRenderer.render(qualifierDescriptor), expression)
                }
            }
        }

        override fun visitIfExpression(expression: KtIfExpression) {
            addAnnotation(renderType(expression), expression.ifKeyword)
            super.visitIfExpression(expression)
        }

        override fun visitWhenExpression(expression: KtWhenExpression) {
            addAnnotation(renderType(expression), expression.whenKeyword)
            super.visitWhenExpression(expression)
        }

        override fun visitWhenEntry(jetWhenEntry: KtWhenEntry) {
            addAnnotation(renderType(jetWhenEntry.expression), jetWhenEntry.expression!!)
            super.visitWhenEntry(jetWhenEntry)
        }
    }
}
