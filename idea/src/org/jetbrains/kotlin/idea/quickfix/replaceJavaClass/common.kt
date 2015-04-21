/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.quickfix.replaceJavaClass

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.types.typeUtil.isArrayOfJavaLangClass
import org.jetbrains.kotlin.types.typeUtil.isJavaLangClass

private trait ReplacementTask {
    val element: JetElement
}
private class JavaClassCallReplacementTask(
        val javaClassCall: JetExpression,
        val className: String
) : ReplacementTask {
    override val element: JetElement = javaClassCall
}

fun createReplacementTasks(element: JetAnnotationEntry): List<ReplacementTask> {
    val replacementTasks = arrayListOf<ReplacementTask>()

    element.accept(object : JetTreeVisitorVoid() {
        override fun visitCallExpression(expression: JetCallExpression) {
            expression.acceptChildren(this)

            val context = expression.analyze()
            val resolvedCall = expression.getResolvedCall(context) ?: return

            if (!context.getDiagnostics().any { it.isJavaLangClassArgumentInAnnotation(expression) }) return

            val returnType = resolvedCall.getResultingDescriptor().getReturnType() ?: return
            if (returnType.isJavaLangClass()) {
                val inferredType = returnType.getArguments().firstOrNull()?.getType() ?: return
                if (inferredType.isError()) return
                val renderedType = IdeDescriptorRenderers.SOURCE_CODE.renderType(inferredType)
                replacementTasks.add(JavaClassCallReplacementTask(expression, renderedType))
            }
        }
    })

    return replacementTasks
}

private fun Diagnostic.isJavaLangClassArgumentInAnnotation(expression: JetCallExpression) =
        getFactory() == ErrorsJvm.JAVA_LANG_CLASS_ARGUMENT_IN_ANNOTATION &&
        getPsiElement().isAncestor(expression)

fun processTasks(replacementTasks: Collection<ReplacementTask>) {
    val element = replacementTasks.firstOrNull()?.element ?: return
    val psiFactory = JetPsiFactory(element)

    val elementsToShorten = arrayListOf<JetElement>()
    replacementTasks.forEach {
        task ->
        when (task) {
            is JavaClassCallReplacementTask -> {
                val newElement = task.javaClassCall.replace(psiFactory.createClassLiteral(task.className)) as JetElement
                elementsToShorten.add(newElement)
            }
            else -> error("Unexpected task type: ${task.javaClass.getName()}")
        }
    }

    ShortenReferences.DEFAULT.process(elementsToShorten)
}
