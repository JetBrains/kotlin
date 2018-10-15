/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.java

import com.intellij.codeInsight.ClassUtil.getAnyMethodToImplement
import com.intellij.codeInsight.daemon.JavaErrorMessages
import com.intellij.codeInsight.daemon.impl.analysis.HighlightNamesUtil.getClassDeclarationTextRange
import com.intellij.codeInsight.daemon.impl.analysis.HighlightUtil
import com.intellij.codeInsight.daemon.impl.analysis.JavaHighlightUtil
import com.intellij.codeInsight.intention.QuickFixFactory
import com.intellij.lang.annotation.Annotation
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.classes.KtLightClassForSourceDeclaration
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.resolve.jvm.annotations.JVM_DEFAULT_FQ_NAME
import org.jetbrains.kotlin.utils.ifEmpty

class UnimplementedKotlinInterfaceMemberAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is PsiClass || element.language == KotlinLanguage.INSTANCE) return

        if (element.isInterface || element.hasModifierProperty(PsiModifier.ABSTRACT)) return

        if (getAnyMethodToImplement(element) != null) return // reported by java default annotator

        findUnimplementedMethod(element)?.let {
            report(it, holder, element)
        }

    }

    private fun findUnimplementedMethod(psiClass: PsiClass): KtLightMethod? {
        val signaturesFromKotlinInterfaces = psiClass.visibleSignatures.filter { signature ->
            signature.method.let { it is KtLightMethod && it.hasModifierProperty(PsiModifier.DEFAULT) }
        }.ifEmpty { return null }

        val kotlinSuperClass = generateSequence(psiClass) { it.superClass }.firstOrNull { it is KtLightClassForSourceDeclaration }

        val signaturesVisibleThroughKotlinSuperClass = kotlinSuperClass?.visibleSignatures ?: emptyList()
        return signaturesFromKotlinInterfaces.firstOrNull {
            it !in signaturesVisibleThroughKotlinSuperClass &&
                    it.method.modifierList.annotations.none { annotation ->
                        annotation.qualifiedName == JVM_DEFAULT_FQ_NAME.asString()
                    }
        }?.method as? KtLightMethod
    }

    private fun report(method: KtLightMethod, holder: AnnotationHolder, psiClass: PsiClass) {
        val key = if (psiClass is PsiEnumConstantInitializer) "enum.constant.should.implement.method" else "class.must.be.abstract"
        val message = JavaErrorMessages.message(key, HighlightUtil.formatClass(psiClass, false), JavaHighlightUtil.formatMethod(method),
                                                HighlightUtil.formatClass(method.containingClass, false))
        val errorAnnotation = holder.createErrorAnnotation(getClassDeclarationTextRange(psiClass), message)
        registerFixes(errorAnnotation, psiClass)
    }

    private fun registerFixes(errorAnnotation: Annotation, psiClass: PsiClass) {
        val quickFixFactory = QuickFixFactory.getInstance()
        // this code is untested
        // see com.intellij.codeInsight.daemon.impl.analysis.HighlightClassUtil.checkClassWithAbstractMethods
        errorAnnotation.registerFix(quickFixFactory.createImplementMethodsFix(psiClass))
        if (psiClass !is PsiAnonymousClass && !(psiClass.modifierList?.hasExplicitModifier(PsiModifier.FINAL) ?: false)) {
            errorAnnotation.registerFix(quickFixFactory.createModifierListFix(psiClass, PsiModifier.ABSTRACT, true, false))
        }
    }
}