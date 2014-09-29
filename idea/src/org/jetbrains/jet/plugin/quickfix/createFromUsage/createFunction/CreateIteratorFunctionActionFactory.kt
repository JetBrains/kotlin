package org.jetbrains.jet.plugin.quickfix.createFromUsage.createFunction

import org.jetbrains.jet.plugin.quickfix.JetSingleIntentionActionFactory
import org.jetbrains.jet.lang.diagnostics.Diagnostic
import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.plugin.quickfix.QuickFixUtil
import org.jetbrains.jet.lang.psi.JetForExpression
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.types.Variance
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.plugin.caches.resolve.getBindingContext
import org.jetbrains.jet.lang.types.TypeProjectionImpl
import java.util.Collections
import org.jetbrains.jet.lang.types.JetTypeImpl
import org.jetbrains.jet.plugin.quickfix.createFromUsage.callableBuilder.*

object CreateIteratorFunctionActionFactory : JetSingleIntentionActionFactory() {
    override fun createAction(diagnostic: Diagnostic): IntentionAction? {
        val file = diagnostic.getPsiFile() as? JetFile ?: return null
        val forExpr = QuickFixUtil.getParentElementOfType(diagnostic, javaClass<JetForExpression>()) ?: return null
        val iterableExpr = forExpr.getLoopRange() ?: return null
        val variableExpr: JetExpression = ((forExpr.getLoopParameter() ?: forExpr.getMultiParameter()) ?: return null) as JetExpression
        val iterableType = TypeInfo(iterableExpr, Variance.IN_VARIANCE)
        val returnJetType = KotlinBuiltIns.getInstance().getIterator().getDefaultType()

        val context = file.getBindingContext()
        val returnJetTypeParameterTypes = variableExpr.guessTypes(context)
        if (returnJetTypeParameterTypes.size != 1) return null

        val returnJetTypeParameterType = TypeProjectionImpl(returnJetTypeParameterTypes[0])
        val returnJetTypeArguments = Collections.singletonList(returnJetTypeParameterType)
        val newReturnJetType = JetTypeImpl(returnJetType.getAnnotations(), returnJetType.getConstructor(), returnJetType.isNullable(), returnJetTypeArguments, returnJetType.getMemberScope())
        val returnType = TypeInfo(newReturnJetType, Variance.OUT_VARIANCE)
        return CreateFunctionFromUsageFix(forExpr, createFunctionInfo("iterator", iterableType, returnType))
    }
}
