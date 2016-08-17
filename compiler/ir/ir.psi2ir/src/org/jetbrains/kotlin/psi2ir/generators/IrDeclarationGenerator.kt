/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.toExpectedType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.singletonList

class IrDeclarationGenerator(override val context: IrGeneratorContext) : IrGenerator {
    fun generateAnnotationEntries(annotationEntries: List<KtAnnotationEntry>) {
        // TODO create IrAnnotation's for each KtAnnotationEntry
    }

    fun generateMemberDeclaration(ktDeclaration: KtDeclaration): IrDeclaration =
            when (ktDeclaration) {
                is KtNamedFunction ->
                    generateFunctionDeclaration(ktDeclaration)
                is KtProperty ->
                    generatePropertyDeclaration(ktDeclaration)
                is KtClassOrObject ->
                    generateClassOrObjectDeclaration(ktDeclaration)
                is KtTypeAlias ->
                    generateTypeAliasDeclaration(ktDeclaration)
                else ->
                    IrDummyDeclaration(
                            ktDeclaration.startOffset, ktDeclaration.endOffset,
                            getOrFail(BindingContext.DECLARATION_TO_DESCRIPTOR, ktDeclaration)
                    )
            }

    fun generateClassOrObjectDeclaration(ktDeclaration: KtClassOrObject): IrDeclaration =
            IrDummyDeclaration(
                    ktDeclaration.startOffset, ktDeclaration.endOffset,
                    getOrFail(BindingContext.CLASS, ktDeclaration)
            )

    fun generateTypeAliasDeclaration(ktDeclaration: KtTypeAlias): IrDeclaration =
            IrDummyDeclaration(
                    ktDeclaration.startOffset, ktDeclaration.endOffset,
                    getOrFail(BindingContext.TYPE_ALIAS, ktDeclaration)
            )

    fun generateFunctionDeclaration(ktNamedFunction: KtNamedFunction): IrFunction {
        val functionDescriptor = getOrFail(BindingContext.FUNCTION, ktNamedFunction)
        val body = generateFunctionBody(functionDescriptor, ktNamedFunction.bodyExpression ?: TODO("function without body expression"))
        return createFunction(ktNamedFunction, functionDescriptor, body)
    }

    fun generateLocalVariable(scopeOwner: DeclarationDescriptor, ktProperty: KtProperty): IrVariable {
        if (ktProperty.delegateExpression != null) TODO("Local delegated property")

        val variableDescriptor = getOrFail(BindingContext.VARIABLE, ktProperty)

        val irLocalVariable = IrVariableImpl(ktProperty.startOffset, ktProperty.endOffset, IrDeclarationOriginKind.DEFINED, variableDescriptor)

        irLocalVariable.initializer = ktProperty.initializer?.let {
            generateExpressionWithinContext(it, scopeOwner)
        }

        return irLocalVariable
    }

    fun generatePropertyDeclaration(ktProperty: KtProperty): IrProperty {
        val propertyDescriptor = getPropertyDescriptor(ktProperty)
        if (ktProperty.hasDelegate()) TODO("handle delegated property")
        val initializer = ktProperty.initializer?.let { generateInitializerBody(propertyDescriptor, it) }
        val irProperty = createSimpleProperty(ktProperty, propertyDescriptor, initializer)

        ktProperty.getter?.let { ktGetter ->
            val accessorDescriptor = getOrFail(BindingContext.PROPERTY_ACCESSOR, ktGetter)
            val getterDescriptor = accessorDescriptor as? PropertyGetterDescriptor ?: TODO("not a getter?")
            val getterBody = generateFunctionBody(getterDescriptor, ktGetter.bodyExpression ?: TODO("default getter"))
            createPropertyGetter(ktGetter, irProperty, getterDescriptor, getterBody)
        }

        ktProperty.setter?.let { ktSetter ->
            val accessorDescriptor = getOrFail(BindingContext.PROPERTY_ACCESSOR, ktSetter)
            val setterDescriptor = accessorDescriptor as? PropertySetterDescriptor ?: TODO("not a setter?")
            val setterBody = generateFunctionBody(setterDescriptor, ktSetter.bodyExpression ?: TODO("default setter"))
            createPropertySetter(ktSetter, irProperty, setterDescriptor, setterBody)
        }

        return irProperty
    }

    fun createFunction(ktFunction: KtFunction, functionDescriptor: FunctionDescriptor, body: IrBody): IrFunction =
            IrFunctionImpl(ktFunction.startOffset, ktFunction.endOffset,
                           IrDeclarationOriginKind.DEFINED, functionDescriptor, body)

    fun createSimpleProperty(ktProperty: KtProperty, propertyDescriptor: PropertyDescriptor, valueInitializer: IrBody?): IrSimpleProperty =
            IrSimplePropertyImpl(ktProperty.startOffset, ktProperty.endOffset,
                                 IrDeclarationOriginKind.DEFINED, propertyDescriptor, valueInitializer)

    fun createPropertyGetter(
            ktPropertyAccessor: KtPropertyAccessor,
            irProperty: IrProperty,
            getterDescriptor: PropertyGetterDescriptor,
            getterBody: IrBody
    ): IrPropertyGetter =
            IrPropertyGetterImpl(ktPropertyAccessor.startOffset, ktPropertyAccessor.endOffset,
                                 IrDeclarationOriginKind.DEFINED, getterDescriptor, getterBody)
                    .apply { irProperty.getter = this }

    fun createPropertySetter(
            ktPropertyAccessor: KtPropertyAccessor,
            irProperty: IrProperty,
            setterDescriptor: PropertySetterDescriptor,
            setterBody: IrBody
    ) : IrPropertySetter =
            IrPropertySetterImpl(ktPropertyAccessor.startOffset, ktPropertyAccessor.endOffset,
                                 IrDeclarationOriginKind.DEFINED, setterDescriptor, setterBody)
                    .apply { irProperty.setter = this }

    private fun getPropertyDescriptor(ktProperty: KtProperty): PropertyDescriptor {
        val variableDescriptor = getOrFail(BindingContext.VARIABLE, ktProperty)
        val propertyDescriptor = variableDescriptor as? PropertyDescriptor ?: TODO("not a property?")
        return propertyDescriptor
    }

    private fun generateExpressionWithinContext(ktExpression: KtExpression, scopeOwner: DeclarationDescriptor): IrExpression =
            IrStatementGenerator(context, scopeOwner, IrTemporaryVariableFactory(scopeOwner))
                    .generateExpression(ktExpression)
                    .toExpectedType(getExpectedTypeForLastInferredCall(ktExpression))

    private fun generateFunctionBody(scopeOwner: CallableDescriptor, ktBody: KtExpression): IrBody {
        val irRhs = generateExpressionWithinContext(ktBody, scopeOwner)
        val irExpressionBody =
                if (ktBody is KtBlockExpression)
                    irRhs
                else
                    IrBlockExpressionImpl(ktBody.startOffset, ktBody.endOffset, null, hasResult = false, isDesugared = true).apply {
                        addStatement(IrReturnExpressionImpl(ktBody.startOffset, ktBody.endOffset, scopeOwner, irRhs))
                    }
        return IrExpressionBodyImpl(ktBody.startOffset, ktBody.endOffset, irExpressionBody)
    }

    private fun generateInitializerBody(scopeOwner: DeclarationDescriptor, ktBody: KtExpression): IrBody =
            IrExpressionBodyImpl(ktBody.startOffset, ktBody.endOffset,
                                 generateExpressionWithinContext(ktBody, scopeOwner))
}