/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compiler.visualizer

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.compiler.visualizer.Annotator.annotate
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.renderer.ClassifierNamePolicy
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContext.*
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe

class PsiRenderer(private val file: KtFile, analysisResult: AnalysisResult): BaseRenderer {
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
    }

    override fun render(): String {
        file.accept(Renderer())
        return annotate(file.text, annotations).joinToString("\n")
    }

    inner class Renderer : KtVisitorVoid() {
        override fun visitElement(element: PsiElement) {
            element.acceptChildren(this)
        }

        override fun visitKtElement(element: KtElement) {
            element.acceptChildren(this)
        }

        override fun visitProperty(property: KtProperty) {
            val descriptor = bindingContext[VARIABLE, property]
            annotations.add(Annotator.AnnotationInfo(descriptor?.returnType.toString(), property.nameIdentifier!!.textRange))
            super.visitProperty(property)
        }

        override fun visitConstantExpression(expression: KtConstantExpression) {
            val type = bindingContext.getType(expression)
            annotations.add(Annotator.AnnotationInfo(type.toString(), expression.textRange))
        }

        override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
            val resolvedCall = bindingContext[RESOLVED_CALL, bindingContext[CALL, expression]]
            if (resolvedCall == null || resolvedCall.resultingDescriptor == null) {
                super.visitSimpleNameExpression(expression)
                return
            }

            val annotation = descriptorRenderer.render(resolvedCall.resultingDescriptor)
            val fqName = DescriptorUtils.getFqName(resolvedCall.resultingDescriptor.containingDeclaration)
            annotations.add(Annotator.AnnotationInfo("$fqName.$annotation", expression.textRange))
        }
    }
}
