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
import org.jetbrains.jet.lang.psi.JetClass
import org.jetbrains.jet.plugin.quickfix.createFromUsage.callableBuilder.guessTypes
import org.jetbrains.jet.plugin.caches.resolve.getBindingContext
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import org.jetbrains.jet.plugin.refactoring.changeSignature.runChangeSignature
import org.jetbrains.jet.plugin.refactoring.changeSignature.JetChangeSignatureConfiguration
import org.jetbrains.jet.plugin.refactoring.changeSignature.JetChangeSignatureData
import org.jetbrains.jet.plugin.refactoring.changeSignature.JetParameterInfo
import com.intellij.psi.PsiElement
import org.jetbrains.jet.lang.psi.JetPropertyAccessor
import org.jetbrains.jet.lang.psi.JetFunction
import org.jetbrains.jet.lang.psi.JetClassInitializer
import org.jetbrains.jet.lang.psi.JetClassBody
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.psi.psiUtil.parents
import org.jetbrains.jet.plugin.quickfix.createFromUsage.callableBuilder.getTypeParameters
import org.jetbrains.jet.lang.descriptors.ClassDescriptorWithResolutionScopes
import java.util.LinkedHashSet
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor
import org.jetbrains.jet.plugin.refactoring.changeSignature.JetValVar
import org.jetbrains.jet.lang.psi.JetEnumEntry
import org.jetbrains.jet.lang.psi.JetClassOrObject
import org.jetbrains.jet.lang.resolve.DescriptorToSourceUtils
import org.jetbrains.jet.lang.psi.JetNamedFunction
import org.jetbrains.jet.lang.psi.psiUtil.getAssignmentByLHS
import org.jetbrains.jet.plugin.quickfix.createFromUsage.callableBuilder.getExpressionForTypeGuess
import org.jetbrains.jet.plugin.caches.resolve.getAnalysisResults

object CreateParameterActionFactory: JetSingleIntentionActionFactory() {
    private fun JetType.hasTypeParametersToAdd(functionDescriptor: FunctionDescriptor, context: BindingContext): Boolean {
        val typeParametersToAdd = LinkedHashSet(getTypeParameters())
        typeParametersToAdd.removeAll(functionDescriptor.getTypeParameters())
        if (typeParametersToAdd.isEmpty()) return false

        val scope = when(functionDescriptor) {
            is ConstructorDescriptor -> {
                val classDescriptor = (functionDescriptor as? ConstructorDescriptor)?.getContainingDeclaration()
                (classDescriptor as? ClassDescriptorWithResolutionScopes)?.getScopeForClassHeaderResolution()
            }

            is FunctionDescriptor -> {
                val function = DescriptorToSourceUtils.descriptorToDeclaration(functionDescriptor) as? JetFunction
                function?.let { context[BindingContext.RESOLUTION_SCOPE, it.getBodyExpression()] }
            }

            else -> null
        } ?: return true

        return typeParametersToAdd.any { scope.getClassifier(it.getName()) != it }
    }

    override fun createAction(diagnostic: Diagnostic): IntentionAction? {
        val exhaust = (diagnostic.getPsiFile() as? JetFile)?.getAnalysisResults() ?: return null
        val context = exhaust.getBindingContext()

        val refExpr = QuickFixUtil.getParentElementOfType(diagnostic, javaClass<JetSimpleNameExpression>()) ?: return null
        if (refExpr.getQualifiedElement() != refExpr) return null

        val varExpected = refExpr.getAssignmentByLHS() != null

        val paramType = refExpr.getExpressionForTypeGuess().guessTypes(context, exhaust.getModuleDescriptor()).let {
            when (it.size) {
                0 -> KotlinBuiltIns.getInstance().getAnyType()
                1 -> it.first()
                else -> return null
            }
        }

        val parameterInfo = JetParameterInfo(refExpr.getReferencedName(), paramType)

        fun chooseContainingClass(it: PsiElement): JetClass? {
            parameterInfo.setValOrVar(if (varExpected) JetValVar.Var else JetValVar.Val)
            return it.parents(false).filterIsInstance(javaClass<JetClassOrObject>()).firstOrNull() as? JetClass
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

        return object: CreateFromUsageFixBase(refExpr) {
            override fun getText(): String {
                return JetBundle.message("create.parameter.from.usage", refExpr.getReferencedName())
            }

            override fun invoke(project: Project, editor: Editor?, file: JetFile?) {
                val config = object : JetChangeSignatureConfiguration {
                    override fun configure(changeSignatureData: JetChangeSignatureData, bindingContext: BindingContext) {
                        changeSignatureData.addParameter(parameterInfo)
                    }

                    override fun performSilently(affectedFunctions: Collection<PsiElement>): Boolean = false
                }

                runChangeSignature(project, functionDescriptor, config, context, refExpr, getText())
            }
        }
    }
}
