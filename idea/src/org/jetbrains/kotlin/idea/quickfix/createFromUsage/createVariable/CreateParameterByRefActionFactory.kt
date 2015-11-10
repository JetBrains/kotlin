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
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptorWithResolutionScopes
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFullyAndGetResult
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.getExpressionForTypeGuess
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.getTypeParameters
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.guessTypes
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinParameterInfo
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinValVar
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getAssignmentByLHS
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElement
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.scopes.utils.findClassifier
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.util.*

object CreateParameterByRefActionFactory : CreateParameterFromUsageFactory<KtSimpleNameExpression>() {
    override fun getElementOfInterest(diagnostic: Diagnostic): KtSimpleNameExpression? {
        val refExpr = QuickFixUtil.getParentElementOfType(diagnostic, javaClass<KtSimpleNameExpression>()) ?: return null
        if (refExpr.getQualifiedElement() != refExpr) return null
        if (refExpr.getReferencedNameElementType() != KtTokens.IDENTIFIER) return null
        return refExpr
    }

    fun extractFixData(element: KtSimpleNameExpression): CreateParameterData<KtSimpleNameExpression>? {
        val result = (element.containingFile as? KtFile)?.analyzeFullyAndGetResult() ?: return null
        val context = result.bindingContext
        val moduleDescriptor = result.moduleDescriptor

        val varExpected = element.getAssignmentByLHS() != null

        val paramType = element.getExpressionForTypeGuess().guessTypes(context, moduleDescriptor).let {
            when (it.size()) {
                0 -> moduleDescriptor.builtIns.anyType
                1 -> it.first()
                else -> return null
            }
        }

        var valOrVar: KotlinValVar = KotlinValVar.None

        fun chooseFunction(): PsiElement? {
            if (varExpected) return null
            return element.parents.filter { it is KtNamedFunction || it is KtSecondaryConstructor }.firstOrNull()
        }

        fun chooseContainingClass(it: PsiElement): KtClass? {
            valOrVar = if (varExpected) KotlinValVar.Var else KotlinValVar.Val
            return it.parents.firstIsInstanceOrNull<KtClassOrObject>() as? KtClass
        }

        // todo: skip lambdas for now because Change Signature doesn't apply to them yet
        fun chooseContainerPreferringClass(): PsiElement? {
            return element.parents
                    .filter {
                        it is KtNamedFunction || it is KtSecondaryConstructor || it is KtPropertyAccessor ||
                        it is KtClassBody || it is KtClassInitializer || it is KtDelegationSpecifier
                    }
                    .firstOrNull()
                    ?.let {
                        when {
                            (it is KtNamedFunction || it is KtSecondaryConstructor) && varExpected,
                            it is KtPropertyAccessor -> chooseContainingClass(it)
                            it is KtClassInitializer -> it.parent?.parent as? KtClass
                            it is KtDelegationSpecifier -> {
                                val klass = it.getStrictParentOfType<KtClassOrObject>()
                                if (klass is KtClass && !klass.isInterface() && klass !is KtEnumEntry) klass else null
                            }
                            it is KtClassBody -> {
                                val klass = it.parent as? KtClass
                                when {
                                    klass is KtEnumEntry -> chooseContainingClass(klass)
                                    klass != null && klass.isInterface() -> null
                                    else -> klass
                                }
                            }
                            else -> it
                        }
                    }
        }

        val container = chooseContainerPreferringClass() ?: chooseFunction()

        val functionDescriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, container]?.let {
            if (it is ClassDescriptor) it.unsubstitutedPrimaryConstructor else it
        } as? FunctionDescriptor ?: return null

        if (paramType.hasTypeParametersToAdd(functionDescriptor, context)) return null

        return CreateParameterData(
                context,
                KotlinParameterInfo(callableDescriptor = functionDescriptor,
                                    name = element.getReferencedName(),
                                    type = paramType,
                                    valOrVar = valOrVar),
                element
        )
    }

    override fun extractFixData(element: KtSimpleNameExpression, diagnostic: Diagnostic) = extractFixData(element)
}

fun KotlinType.hasTypeParametersToAdd(functionDescriptor: FunctionDescriptor, context: BindingContext): Boolean {
    val typeParametersToAdd = LinkedHashSet(getTypeParameters())
    typeParametersToAdd.removeAll(functionDescriptor.typeParameters)
    if (typeParametersToAdd.isEmpty()) return false

    val scope =
            when (functionDescriptor) {
                is ConstructorDescriptor -> {
                    (functionDescriptor.containingDeclaration as? ClassDescriptorWithResolutionScopes)?.scopeForClassHeaderResolution
                }

                is FunctionDescriptor -> {
                    val function = functionDescriptor.source.getPsi() as? KtFunction
                    function?.bodyExpression?.getResolutionScope(context, function!!.getResolutionFacade())
                }

                else -> null
            } ?: return true

    return typeParametersToAdd.any { scope.findClassifier(it.name, NoLookupLocation.FROM_IDE) != it }
}
