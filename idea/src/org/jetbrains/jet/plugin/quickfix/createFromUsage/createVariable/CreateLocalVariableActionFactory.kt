package org.jetbrains.jet.plugin.quickfix.createFromUsage.createVariable

import org.jetbrains.jet.plugin.quickfix.createFromUsage.CreateFromUsageFixBase
import org.jetbrains.jet.plugin.JetBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.plugin.quickfix.JetSingleIntentionActionFactory
import org.jetbrains.jet.lang.diagnostics.Diagnostic
import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.jet.plugin.quickfix.QuickFixUtil
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import org.jetbrains.jet.lang.psi.psiUtil.getQualifiedElement
import org.jetbrains.jet.lang.psi.JetBlockExpression
import org.jetbrains.jet.plugin.quickfix.createFromUsage.callableBuilder.TypeInfo
import org.jetbrains.jet.lang.types.Variance
import org.jetbrains.jet.plugin.quickfix.createFromUsage.callableBuilder.CallableBuilderConfiguration
import org.jetbrains.jet.plugin.quickfix.createFromUsage.callableBuilder.createBuilder
import org.jetbrains.jet.plugin.quickfix.createFromUsage.callableBuilder.CallablePlacement
import com.intellij.openapi.command.CommandProcessor
import org.jetbrains.jet.lang.psi.psiUtil.parents
import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.plugin.intentions.ConvertToBlockBodyAction
import org.jetbrains.jet.lang.psi.JetDeclarationWithBody
import org.jetbrains.jet.plugin.refactoring.getExtractionContainers
import org.jetbrains.jet.lang.psi.JetClassBody
import org.jetbrains.jet.plugin.quickfix.createFromUsage.callableBuilder.PropertyInfo
import org.jetbrains.jet.lang.psi.psiUtil.getAssignmentByLHS
import org.jetbrains.jet.plugin.quickfix.createFromUsage.callableBuilder.getExpressionForTypeGuess
import org.jetbrains.jet.utils.addToStdlib.singletonOrEmptyList

object CreateLocalVariableActionFactory: JetSingleIntentionActionFactory() {
    override fun createAction(diagnostic: Diagnostic): IntentionAction? {
        val refExpr = QuickFixUtil.getParentElementOfType(diagnostic, javaClass<JetSimpleNameExpression>()) ?: return null
        if (refExpr.getQualifiedElement() != refExpr) return null

        val container = refExpr.parents(false)
                .filter { it is JetBlockExpression || it is JetDeclarationWithBody }
                .firstOrNull() as? JetElement ?: return null

        val assignment = refExpr.getAssignmentByLHS()
        val varExpected = assignment != null
        val typeInfo = TypeInfo(
                refExpr.getExpressionForTypeGuess(),
                if (varExpected) Variance.INVARIANT else Variance.OUT_VARIANCE
        )
        val containers = refExpr.getExtractionContainers().filterNot { it is JetClassBody || it is JetFile }
        val propertyInfo = PropertyInfo(refExpr.getReferencedName(), TypeInfo.Empty, typeInfo, varExpected, containers)

        return object: CreateFromUsageFixBase(refExpr) {
            override fun getText(): String = JetBundle.message("create.local.variable.from.usage", propertyInfo.name)

            override fun invoke(project: Project, editor: Editor?, file: JetFile?) {
                with (CallableBuilderConfiguration(propertyInfo.singletonOrEmptyList(), assignment ?: refExpr, file!!, editor!!).createBuilder()) {
                    val actualContainer = when (container) {
                        is JetBlockExpression -> container
                        else -> ConvertToBlockBodyAction().convert(container as JetDeclarationWithBody).getBodyExpression()!!
                    }
                    placement = CallablePlacement.NoReceiver(actualContainer)
                    CommandProcessor.getInstance().executeCommand(project, { build() }, getText(), null)
                }
            }
        }
    }
}