/*
 * Copyright 2000-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.evaluate

import com.intellij.debugger.DebuggerBundle
import com.intellij.debugger.DebuggerInvocationUtil
import com.intellij.debugger.engine.ContextUtil
import com.intellij.debugger.engine.evaluation.EvaluateException
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.engine.evaluation.expression.ExpressionEvaluator
import com.intellij.debugger.impl.DebuggerContextImpl
import com.intellij.debugger.impl.DebuggerUtilsEx
import com.intellij.debugger.ui.EditorEvaluationCommand
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.psi.CommonClassNames
import com.intellij.psi.search.GlobalSearchScope
import com.sun.jdi.ClassType
import com.sun.jdi.Value
import org.jetbrains.eval4j.jdi.asValue
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.debugger.getClassDescriptor
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.org.objectweb.asm.Type as AsmType

abstract class KotlinRuntimeTypeEvaluator(
    editor: Editor?,
    expression: KtExpression,
    context: DebuggerContextImpl,
    indicator: ProgressIndicator
) : EditorEvaluationCommand<KotlinType>(editor, expression, context, indicator) {

    override fun threadAction() {
        var type: KotlinType? = null
        try {
            type = evaluate()
        } catch (ignored: ProcessCanceledException) {
        } catch (ignored: EvaluateException) {
        } finally {
            typeCalculationFinished(type)
        }
    }

    protected abstract fun typeCalculationFinished(type: KotlinType?)

    override fun evaluate(evaluationContext: EvaluationContextImpl): KotlinType? {
        val project = evaluationContext.project

        val evaluator = DebuggerInvocationUtil.commitAndRunReadAction<ExpressionEvaluator>(project) {
            val codeFragment = KtPsiFactory(myElement.project)
                .createExpressionCodeFragment(myElement.text, myElement.containingFile.context)

            val codeFragmentFactory = DebuggerUtilsEx.getCodeFragmentFactory(codeFragment.context, KotlinFileType.INSTANCE)
            codeFragmentFactory.evaluatorBuilder.build(codeFragment, ContextUtil.getSourcePosition(evaluationContext))
        }

        val value = evaluator.evaluate(evaluationContext)
        if (value != null) {
            return runReadAction { getCastableRuntimeType(evaluationContext.debugProcess.searchScope, value) }
        }

        throw EvaluateExceptionUtil.createEvaluateException(DebuggerBundle.message("evaluation.error.surrounded.expression.null"))
    }

    companion object {
        private fun getCastableRuntimeType(scope: GlobalSearchScope, value: Value): KotlinType? {
            val myValue = value.asValue()
            var psiClass = myValue.asmType.getClassDescriptor(scope)
            if (psiClass != null) {
                return psiClass.defaultType
            }

            val type = value.type()
            if (type is ClassType) {
                val superclass = type.superclass()
                if (superclass != null && CommonClassNames.JAVA_LANG_OBJECT != superclass.name()) {
                    psiClass = AsmType.getType(superclass.signature()).getClassDescriptor(scope)
                    if (psiClass != null) {
                        return psiClass.defaultType
                    }
                }

                for (interfaceType in type.interfaces()) {
                    psiClass = AsmType.getType(interfaceType.signature()).getClassDescriptor(scope)
                    if (psiClass != null) {
                        return psiClass.defaultType
                    }
                }
            }
            return null
        }
    }
}

