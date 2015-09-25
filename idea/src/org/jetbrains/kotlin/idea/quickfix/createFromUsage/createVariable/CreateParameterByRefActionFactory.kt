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

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createVariable

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptorWithResolutionScopes
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFullyAndGetResult
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.getExpressionForTypeGuess
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.getTypeParameters
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.guessTypes
import org.jetbrains.kotlin.idea.refactoring.changeSignature.JetParameterInfo
import org.jetbrains.kotlin.idea.refactoring.changeSignature.JetValVar
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getAssignmentByLHS
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElement
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.scopes.utils.asJetScope
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.util.*

object CreateParameterByRefActionFactory : CreateParameterFromUsageFactory<JetSimpleNameExpression>() {
    override fun getElementOfInterest(diagnostic: Diagnostic): JetSimpleNameExpression? {
        val refExpr = QuickFixUtil.getParentElementOfType(diagnostic, javaClass<JetSimpleNameExpression>()) ?: return null
        if (refExpr.getQualifiedElement() != refExpr) return null
        if (refExpr.getReferencedNameElementType() != JetTokens.IDENTIFIER) return null
        return refExpr
    }

    override fun createQuickFixData(
            element: JetSimpleNameExpression,
            diagnostic: Diagnostic
    ): CreateParameterData<JetSimpleNameExpression>? {
        val result = (diagnostic.psiFile as? JetFile)?.analyzeFullyAndGetResult() ?: return null
        val context = result.bindingContext

        val varExpected = element.getAssignmentByLHS() != null

        val paramType = element.getExpressionForTypeGuess().guessTypes(context, result.moduleDescriptor).let {
            when (it.size()) {
                0 -> KotlinBuiltIns.getInstance().anyType
                1 -> it.first()
                else -> return null
            }
        }

        var valOrVar: JetValVar = JetValVar.None

        fun chooseContainingClass(it: PsiElement): JetClass? {
            valOrVar = if (varExpected) JetValVar.Var else JetValVar.Val
            return it.parents.firstIsInstanceOrNull<JetClassOrObject>() as? JetClass
        }

        // todo: skip lambdas for now because Change Signature doesn't apply to them yet
        val container = element.parents
                                .filter {
                                    it is JetNamedFunction || it is JetPropertyAccessor || it is JetClassBody || it is JetClassInitializer ||
                                    it is JetDelegationSpecifier
                                }
                                .firstOrNull()
                                ?.let {
                                    when {
                                        it is JetNamedFunction && varExpected,
                                        it is JetPropertyAccessor -> chooseContainingClass(it)
                                        it is JetClassInitializer -> it.parent?.parent as? JetClass
                                        it is JetDelegationSpecifier -> {
                                            val klass = it.getStrictParentOfType<JetClass>()
                                            if (klass != null && !klass.isInterface() && klass !is JetEnumEntry) klass else null
                                        }
                                        it is JetClassBody -> {
                                            val klass = it.parent as? JetClass
                                            when {
                                                klass is JetEnumEntry -> chooseContainingClass(klass)
                                                klass != null && klass.isInterface() -> null
                                                else -> klass
                                            }
                                        }
                                        else -> it
                                    }
                                } ?: return null

        val functionDescriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, container]?.let {
            if (it is ClassDescriptor) it.unsubstitutedPrimaryConstructor else it
        } as? FunctionDescriptor ?: return null

        if (paramType.hasTypeParametersToAdd(functionDescriptor, context)) return null

        return CreateParameterData(
                context,
                JetParameterInfo(callableDescriptor = functionDescriptor,
                                 name = element.getReferencedName(),
                                 type = paramType,
                                 valOrVar = valOrVar),
                element
        )
    }
}

fun JetType.hasTypeParametersToAdd(functionDescriptor: FunctionDescriptor, context: BindingContext): Boolean {
    val typeParametersToAdd = LinkedHashSet(getTypeParameters())
    typeParametersToAdd.removeAll(functionDescriptor.typeParameters)
    if (typeParametersToAdd.isEmpty()) return false

    val scope =
            when (functionDescriptor) {
                is ConstructorDescriptor -> {
                    (functionDescriptor.containingDeclaration as? ClassDescriptorWithResolutionScopes)?.scopeForClassHeaderResolution?.asJetScope()
                }

                is FunctionDescriptor -> {
                    val function = functionDescriptor.source.getPsi() as? JetFunction
                    function?.let { context[BindingContext.RESOLUTION_SCOPE, it.bodyExpression] }
                }

                else -> null
            } ?: return true

    return typeParametersToAdd.any { scope.getClassifier(it.name, NoLookupLocation.FROM_IDE) != it }
}
