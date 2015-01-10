package org.jetbrains.jet.plugin.quickfix.createFromUsage.createFunction

import org.jetbrains.jet.plugin.quickfix.JetSingleIntentionActionFactory
import org.jetbrains.kotlin.diagnostics.Diagnostic
import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.jet.plugin.quickfix.QuickFixUtil
import org.jetbrains.kotlin.psi.JetForExpression
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.jet.plugin.caches.resolve.analyzeFully
import org.jetbrains.kotlin.types.TypeProjectionImpl
import java.util.Collections
import org.jetbrains.kotlin.types.JetTypeImpl
import org.jetbrains.jet.plugin.quickfix.createFromUsage.callableBuilder.*

object CreateIteratorFunctionActionFactory : JetSingleIntentionActionFactory() {
    override fun createAction(diagnostic: Diagnostic): IntentionAction? {
        val file = diagnostic.getPsiFile() as? JetFile ?: return null
        val forExpr = QuickFixUtil.getParentElementOfType(diagnostic, javaClass<JetForExpression>()) ?: return null
        val iterableExpr = forExpr.getLoopRange() ?: return null
        val variableExpr: JetExpression = ((forExpr.getLoopParameter() ?: forExpr.getMultiParameter()) ?: return null) as JetExpression
        val iterableType = TypeInfo(iterableExpr, Variance.IN_VARIANCE)
        val returnJetType = KotlinBuiltIns.getInstance().getIterator().getDefaultType()

        val context = file.analyzeFully()
        val returnJetTypeParameterTypes = variableExpr.guessTypes(context, null)
        if (returnJetTypeParameterTypes.size != 1) return null

        val returnJetTypeParameterType = TypeProjectionImpl(returnJetTypeParameterTypes[0])
        val returnJetTypeArguments = Collections.singletonList(returnJetTypeParameterType)
        val newReturnJetType = JetTypeImpl(returnJetType.getAnnotations(), returnJetType.getConstructor(), returnJetType.isMarkedNullable(), returnJetTypeArguments, returnJetType.getMemberScope())
        val returnType = TypeInfo(newReturnJetType, Variance.OUT_VARIANCE)
        return CreateCallableFromUsageFix(forExpr, FunctionInfo("iterator", iterableType, returnType))
    }
}
