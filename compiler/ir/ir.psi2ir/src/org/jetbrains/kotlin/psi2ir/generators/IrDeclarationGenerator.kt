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

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDummyDeclaration
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext

interface IrDeclarationGenerator : IrGenerator

abstract class IrDeclarationGeneratorBase(
        override val context: IrGeneratorContext,
        val declarationFactory: IrDeclarationFactoryBase
) : IrDeclarationGenerator {
    protected abstract fun <D : IrDeclaration> D.register(): D

    fun generateAnnotationEntries(annotationEntries: List<KtAnnotationEntry>) {
        // TODO create IrAnnotation's for each KtAnnotationEntry
    }

    fun generateMemberDeclaration(ktDeclaration: KtDeclaration) {
        // TODO visitor?
        when (ktDeclaration) {
            is KtNamedFunction -> generateFunctionDeclaration(ktDeclaration)
            is KtProperty -> generatePropertyDeclaration(ktDeclaration)
            is KtClassOrObject -> generateClassOrObjectDeclaration(ktDeclaration)
            is KtTypeAlias -> generateTypeAliasDeclaration(ktDeclaration)
        }
    }

    private fun generateClassOrObjectDeclaration(ktDeclaration: KtClassOrObject) {
        IrDummyDeclaration(ktDeclaration.startOffset, ktDeclaration.endOffset, getOrFail(BindingContext.CLASS, ktDeclaration))
                .register()
    }

    private fun generateTypeAliasDeclaration(ktDeclaration: KtTypeAlias) {
        IrDummyDeclaration(ktDeclaration.startOffset, ktDeclaration.endOffset, getOrFail(BindingContext.TYPE_ALIAS, ktDeclaration))
                .register()
    }

    fun generateFunctionDeclaration(ktNamedFunction: KtNamedFunction) {
        val functionDescriptor = getOrFail(BindingContext.FUNCTION, ktNamedFunction)
        val body = generateFunctionBody(functionDescriptor, ktNamedFunction.bodyExpression ?: TODO("function without body expression"))
        declarationFactory.createFunction(ktNamedFunction, functionDescriptor, body).register()
    }

    fun generatePropertyDeclaration(ktProperty: KtProperty) {
        val propertyDescriptor = getPropertyDescriptor(ktProperty)
        if (ktProperty.hasDelegate()) TODO("handle delegated property")
        val initializer = ktProperty.initializer?.let { generateInitializerBody(propertyDescriptor, it) }
        val irProperty = declarationFactory.createSimpleProperty(ktProperty, propertyDescriptor, initializer).register()
        ktProperty.getter?.let { ktGetter ->
            val accessorDescriptor = getOrFail(BindingContext.PROPERTY_ACCESSOR, ktGetter)
            val getterDescriptor = accessorDescriptor as? PropertyGetterDescriptor ?: TODO("not a getter?")
            val getterBody = generateFunctionBody(getterDescriptor, ktGetter.bodyExpression ?: TODO("default getter"))
            declarationFactory.createPropertyGetter(ktGetter, irProperty, getterDescriptor, getterBody).register()
        }
        ktProperty.setter?.let { ktSetter ->
            val accessorDescriptor = getOrFail(BindingContext.PROPERTY_ACCESSOR, ktSetter)
            val setterDescriptor = accessorDescriptor as? PropertySetterDescriptor ?: TODO("not a setter?")
            val setterBody = generateFunctionBody(setterDescriptor, ktSetter.bodyExpression ?: TODO("default setter"))
            declarationFactory.createPropertySetter(ktSetter, irProperty, setterDescriptor, setterBody).register()
        }
    }

    private fun getPropertyDescriptor(ktProperty: KtProperty): PropertyDescriptor {
        val variableDescriptor = getOrFail(BindingContext.VARIABLE, ktProperty)
        val propertyDescriptor = variableDescriptor as? PropertyDescriptor ?: TODO("not a property?")
        return propertyDescriptor
    }

    private fun generateExpressionWithinContext(ktExpression: KtExpression, scopeOwner: DeclarationDescriptor): IrExpression =
            IrStatementGenerator(context, IrLocalDeclarationsFactory(scopeOwner)).generateExpression(ktExpression)

    private fun generateFunctionBody(scopeOwner: DeclarationDescriptor, ktBody: KtExpression): IrBody {
        val irRhs = generateExpressionWithinContext(ktBody, scopeOwner)
        val irExpressionBody =
                if (ktBody is KtBlockExpression)
                    irRhs
                else
                    IrBlockExpressionImpl(ktBody.startOffset, ktBody.endOffset, null, hasResult = false, isDesugared = true).apply {
                        addStatement(IrReturnExpressionImpl(ktBody.startOffset, ktBody.endOffset, null, irRhs))
                    }
        return IrExpressionBodyImpl(ktBody.startOffset, ktBody.endOffset, irExpressionBody)
    }

    private fun generateInitializerBody(scopeOwner: DeclarationDescriptor, ktBody: KtExpression): IrBody =
            IrExpressionBodyImpl(ktBody.startOffset, ktBody.endOffset,
                                 generateExpressionWithinContext(ktBody, scopeOwner))
}