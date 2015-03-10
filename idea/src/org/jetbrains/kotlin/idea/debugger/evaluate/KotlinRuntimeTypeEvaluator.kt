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

package org.jetbrains.kotlin.idea.debugger.evaluate

import com.intellij.debugger.DebuggerBundle
import com.intellij.debugger.DebuggerInvocationUtil
import com.intellij.debugger.EvaluatingComputable
import com.intellij.debugger.engine.ContextUtil
import com.intellij.debugger.engine.evaluation.EvaluateException
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.engine.evaluation.expression.ExpressionEvaluator
import com.intellij.debugger.impl.DebuggerContextImpl
import com.intellij.debugger.ui.EditorEvaluationCommand
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.CommonClassNames
import com.sun.jdi.ClassType
import com.sun.jdi.Value
import org.jetbrains.eval4j.jdi.*
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.org.objectweb.asm.Type as AsmType
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.load.java.JvmAbi

public abstract class KotlinRuntimeTypeEvaluator(
        editor: Editor?,
        expression: JetExpression,
        context: DebuggerContextImpl,
        indicator: ProgressIndicator
) : EditorEvaluationCommand<JetType>(editor, expression, context, indicator) {

    override fun threadAction() {
        var type: JetType? = null
        try {
            type = evaluate()
        }
        catch (ignored: ProcessCanceledException) {
        }
        catch (ignored: EvaluateException) {
        }
        finally {
            typeCalculationFinished(type)
        }
    }

    protected abstract fun typeCalculationFinished(type: JetType?)

    override fun evaluate(evaluationContext: EvaluationContextImpl): JetType? {
        val project = evaluationContext.getProject()

        val evaluator = DebuggerInvocationUtil.commitAndRunReadAction<ExpressionEvaluator>(project, EvaluatingComputable {
                val codeFragment = JetPsiFactory(myElement.getProject()).createExpressionCodeFragment(
                        myElement.getText(), myElement.getContainingFile().getContext())
                KotlinEvaluationBuilder.build(codeFragment, ContextUtil.getSourcePosition(evaluationContext))
        })

        val value = evaluator.evaluate(evaluationContext)
        if (value != null) {
            return getCastableRuntimeType(project, value)
        }

        throw EvaluateExceptionUtil.createEvaluateException(DebuggerBundle.message("evaluation.error.surrounded.expression.null"))
    }

    default object {
        private fun getCastableRuntimeType(project: Project, value: Value): JetType? {
            val myValue = value.asValue()
            var psiClass = myValue.asmType.getClassDescriptor(project)
            if (psiClass != null) {
                return psiClass!!.getDefaultType()
            }

            val type = value.type()
            if (type is ClassType) {
                val superclass = type.superclass()
                if (superclass != null && CommonClassNames.JAVA_LANG_OBJECT != superclass.name()) {
                    psiClass = AsmType.getType(superclass.signature()).getClassDescriptor(project)
                    if (psiClass != null) {
                        return psiClass!!.getDefaultType()
                    }
                }

                for (interfaceType in type.interfaces()) {
                    if (JvmAbi.K_OBJECT.asString() == interfaceType.name()) continue

                    psiClass = AsmType.getType(interfaceType.signature()).getClassDescriptor(project)
                    if (psiClass != null) {
                        return psiClass!!.getDefaultType()
                    }
                }
            }
            return null
        }
    }
}

