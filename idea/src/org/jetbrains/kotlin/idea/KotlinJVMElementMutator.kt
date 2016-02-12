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

package org.jetbrains.kotlin.idea

import com.intellij.codeInsight.daemon.impl.quickfix.JVMElementMutatorWithEditor
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.editor.Editor
import com.intellij.pom.java.LanguageLevel
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.asJava.KtLightClassForExplicitDeclaration
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.DialogWithTemplateEditor
import org.jetbrains.kotlin.idea.refactoring.createJavaClass
import org.jetbrains.kotlin.idea.util.DialogWithEditor

abstract class KotlinJVMElementFactoryBase : JVMElementFactory {
    override fun createDocCommentFromText(name: String) = throw UnsupportedOperationException()

    override fun createInterface(name: String) = throw UnsupportedOperationException()

    override fun createPrimitiveType(name: String) = throw UnsupportedOperationException()

    override fun createExpressionFromText(text: String, context: PsiElement?) = throw UnsupportedOperationException()

    override fun isValidMethodName(name: String) = throw UnsupportedOperationException()

    override fun createMethodFromText(text: String?, context: PsiElement?) = throw UnsupportedOperationException()

    override fun createType(klass: PsiClass) = throw UnsupportedOperationException()

    override fun createType(klass: PsiClass, substitutor: PsiSubstitutor) = throw UnsupportedOperationException()

    override fun createType(klass: PsiClass, substitutor: PsiSubstitutor, level: LanguageLevel) = throw UnsupportedOperationException()

    override fun createType(klass: PsiClass, substitutor: PsiSubstitutor, level: LanguageLevel, annotations: Array<out PsiAnnotation>) =
            throw UnsupportedOperationException()

    override fun createType(klass: PsiClass, parameter: PsiType?) = throw UnsupportedOperationException()

    override fun createType(klass: PsiClass, vararg parameters: PsiType?) = throw UnsupportedOperationException()

    override fun createField(name: String, type: PsiType) = throw UnsupportedOperationException()

    override fun createParameterList(names: Array<out String>, types: Array<out PsiType>) = throw UnsupportedOperationException()

    override fun createClass(name: String) = throw UnsupportedOperationException()

    override fun createEnum(name: String) = throw UnsupportedOperationException()

    override fun createClassInitializer() = throw UnsupportedOperationException()

    override fun createConstructor() = throw UnsupportedOperationException()

    override fun createConstructor(name: String) = throw UnsupportedOperationException()

    override fun createConstructor(name: String, context: PsiElement?) = throw UnsupportedOperationException()

    override fun createRawSubstitutor(owner: PsiTypeParameterListOwner) = throw UnsupportedOperationException()

    override fun isValidLocalVariableName(name: String) = throw UnsupportedOperationException()

    override fun createAnnotationFromText(text: String, context: PsiElement?) = throw UnsupportedOperationException()

    override fun createReferenceElementByType(type: PsiClassType?) = throw UnsupportedOperationException()

    override fun isValidFieldName(name: String) = throw UnsupportedOperationException()

    override fun createTypeParameterList() = throw UnsupportedOperationException()

    override fun createTypeByFQClassName(fqName: String) = throw UnsupportedOperationException()

    override fun createTypeByFQClassName(fqName: String, searchScope: GlobalSearchScope) = throw UnsupportedOperationException()

    override fun createParameter(name: String, type: PsiType?) = throw UnsupportedOperationException()

    override fun createParameter(name: String, type: PsiType?, context: PsiElement?) = throw UnsupportedOperationException()

    override fun createTypeParameter(name: String?, superTypes: Array<out PsiClassType>?) = throw UnsupportedOperationException()

    override fun createSubstitutor(substitutionMap: MutableMap<PsiTypeParameter, PsiType>) = throw UnsupportedOperationException()

    override fun createMethod(name: String, returnType: PsiType?) = throw UnsupportedOperationException()

    override fun createMethod(name: String, returnType: PsiType?, context: PsiElement?) = throw UnsupportedOperationException()

    override fun isValidClassName(name: String) = throw UnsupportedOperationException()

    override fun isValidParameterName(name: String) = throw UnsupportedOperationException()

    override fun createAnnotationType(name: String) = throw UnsupportedOperationException()
}

class KotlinJVMElementMutator private constructor (
        private val rootElement: PsiElement,
        private val rootElementView: PsiElement,
        private val mainEditor: Editor
) : JVMElementMutatorWithEditor {
    private var isDisposed = false

    private val elementFactory: JVMElementFactory by lazy {
        checkNotDisposed()
        object : KotlinJVMElementFactoryBase() {

        }
    }

    private val dialogWithEditor: DialogWithEditor

    init {
        dialogWithEditor = DialogWithTemplateEditor(rootElement.project, mainEditor, "Create from usage") {
            LightVirtualFile("dummy.java", JavaFileType.INSTANCE, "")
        }.apply {
            psiFile!!.add(rootElementView)
        }
    }

    private fun checkNotDisposed() {
        if (isDisposed) throw IllegalStateException("Mutator is already disposed")
    }

    override fun getFactory() = elementFactory

    override fun getRootView(): PsiElement = rootElementView

    override fun openEditor(element: PsiElement?): Editor? {
        checkNotDisposed()
        if (!dialogWithEditor.isVisible) {
            dialogWithEditor.show()
        }
        return dialogWithEditor.editor
    }

    override fun synchronize() {
        checkNotDisposed()
    }

    override fun dispose() {
        isDisposed = true
    }

    companion object {
        fun create(rootElement: PsiElement, editor: Editor): KotlinJVMElementMutator? {
            val elementView = when (rootElement) {
                is KtLightClassForExplicitDeclaration -> createJavaClass(rootElement.getOrigin(), null)
                else -> return null
            }
            return KotlinJVMElementMutator(rootElement, elementView, editor)
        }
    }
}

class KotlinJVMElementMutatorFactory : JVMElementMutatorFactory {
    override fun createMutator(rootElement: PsiElement, editor: Editor) = KotlinJVMElementMutator.create(rootElement, editor)
}
