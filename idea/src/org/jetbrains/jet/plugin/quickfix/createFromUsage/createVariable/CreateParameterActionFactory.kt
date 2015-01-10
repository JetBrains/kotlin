package org.jetbrains.jet.plugin.quickfix.createFromUsage.createVariable

import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.jet.plugin.quickfix.JetSingleIntentionActionFactory
import org.jetbrains.jet.lang.diagnostics.Diagnostic
import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.jet.plugin.quickfix.QuickFixUtil
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElement
import org.jetbrains.kotlin.psi.JetClass
import org.jetbrains.jet.plugin.quickfix.createFromUsage.callableBuilder.guessTypes
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.jet.plugin.refactoring.changeSignature.JetParameterInfo
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.JetPropertyAccessor
import org.jetbrains.kotlin.psi.JetFunction
import org.jetbrains.kotlin.psi.JetClassInitializer
import org.jetbrains.kotlin.psi.JetClassBody
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.jet.plugin.quickfix.createFromUsage.callableBuilder.getTypeParameters
import org.jetbrains.kotlin.descriptors.ClassDescriptorWithResolutionScopes
import java.util.LinkedHashSet
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.jet.plugin.refactoring.changeSignature.JetValVar
import org.jetbrains.kotlin.psi.JetEnumEntry
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.jet.lang.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.psi.JetNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getAssignmentByLHS
import org.jetbrains.jet.plugin.quickfix.createFromUsage.callableBuilder.getExpressionForTypeGuess
import org.jetbrains.jet.plugin.caches.resolve.analyzeFullyAndGetResult
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

object CreateParameterActionFactory: JetSingleIntentionActionFactory() {
    override fun createAction(diagnostic: Diagnostic): IntentionAction? {
        val result = (diagnostic.getPsiFile() as? JetFile)?.analyzeFullyAndGetResult() ?: return null
        val context = result.bindingContext

        val refExpr = QuickFixUtil.getParentElementOfType(diagnostic, javaClass<JetSimpleNameExpression>()) ?: return null
        if (refExpr.getQualifiedElement() != refExpr) return null

        val varExpected = refExpr.getAssignmentByLHS() != null

        val paramType = refExpr.getExpressionForTypeGuess().guessTypes(context, result.moduleDescriptor).let {
            when (it.size()) {
                0 -> KotlinBuiltIns.getInstance().getAnyType()
                1 -> it.first()
                else -> return null
            }
        }

        val parameterInfo = JetParameterInfo(name = refExpr.getReferencedName(), type = paramType)

        fun chooseContainingClass(it: PsiElement): JetClass? {
            parameterInfo.valOrVar = if (varExpected) JetValVar.Var else JetValVar.Val
            return it.parents(false).firstIsInstanceOrNull<JetClassOrObject>() as? JetClass
        }

        // todo: skip lambdas for now because Change Signature doesn't apply to them yet
        val container = refExpr.parents(false)
                .filter { it is JetNamedFunction || it is JetPropertyAccessor || it is JetClassBody || it is JetClassInitializer }
                .firstOrNull()
                ?.let {
                    when {
                        it is JetNamedFunction && varExpected,
                        it is JetPropertyAccessor -> chooseContainingClass(it)
                        it is JetClassInitializer -> it.getParent()?.getParent() as? JetClass
                        it is JetClassBody -> {
                            val klass = it.getParent() as? JetClass
                            when {
                                klass is JetEnumEntry -> chooseContainingClass(klass)
                                klass != null && klass.isTrait() -> null
                                else -> klass
                            }
                        }
                        else -> it
                    }
                } ?: return null

        val functionDescriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, container]?.let {
            if (it is ClassDescriptor) it.getUnsubstitutedPrimaryConstructor() else it
        } as? FunctionDescriptor ?: return null

        if (paramType.hasTypeParametersToAdd(functionDescriptor, context)) return null

        return CreateParameterFromUsageFix(functionDescriptor, context, parameterInfo, refExpr)
    }
}

fun JetType.hasTypeParametersToAdd(functionDescriptor: FunctionDescriptor, context: BindingContext): Boolean {
    val typeParametersToAdd = LinkedHashSet(getTypeParameters())
    typeParametersToAdd.removeAll(functionDescriptor.getTypeParameters())
    if (typeParametersToAdd.isEmpty()) return false

    val scope = when(functionDescriptor) {
                    is ConstructorDescriptor -> {
                        val classDescriptor = functionDescriptor.getContainingDeclaration() as? ClassDescriptorWithResolutionScopes
                        classDescriptor?.getScopeForClassHeaderResolution()
                    }

                    is FunctionDescriptor -> {
                        val function = DescriptorToSourceUtils.descriptorToDeclaration(functionDescriptor) as? JetFunction
                        function?.let { context[BindingContext.RESOLUTION_SCOPE, it.getBodyExpression()] }
                    }

                    else -> null
                } ?: return true

    return typeParametersToAdd.any { scope.getClassifier(it.getName()) != it }
}
