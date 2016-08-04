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

package org.jetbrains.kotlin.psi2ir

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrReturnExpressionImpl
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext

interface IrDeclarationGenerator : IrGenerator {
    val irDeclaration: IrDeclaration
    val parent: IrDeclarationGenerator?
}

abstract class IrDeclarationGeneratorBase(
        override val context: IrGeneratorContext,
        override val irDeclaration: IrDeclaration,
        override val parent: IrDeclarationGenerator,
        val containingFile: PsiSourceManager.PsiFileEntry
) : IrDeclarationGenerator {
    val irExpressionGenerator = IrExpressionGenerator(context, containingFile)

    fun generateAnnotationEntries(annotationEntries: List<KtAnnotationEntry>) {
        // TODO create IrAnnotation's for each KtAnnotationEntry
    }

    fun generateMemberDeclaration(ktDeclaration: KtDeclaration, containingDeclaration: IrCompoundDeclaration) {
        when (ktDeclaration) {
            is KtNamedFunction ->
                generateFunctionDeclaration(ktDeclaration, containingDeclaration)
            is KtProperty ->
                generatePropertyDeclaration(ktDeclaration, containingDeclaration)
            is KtClassOrObject ->
                TODO("classOrObject")
            is KtTypeAlias ->
                TODO("typealias")
        }
    }

    private fun loc(ktElement: KtElement) = containingFile.getSourceLocationForElement(ktElement)

    fun generateFunctionDeclaration(ktNamedFunction: KtNamedFunction, containingDeclaration: IrCompoundDeclaration) {
        val sourceLocation = loc(ktNamedFunction)
        val functionDescriptor = getOrFail(BindingContext.FUNCTION, ktNamedFunction) { "unresolved fun" }
        val body = generateExpressionBody(ktNamedFunction.bodyExpression ?: TODO("function without body expression"))
        val irFunction = IrFunctionImpl(sourceLocation, containingDeclaration, functionDescriptor, body)
        containingDeclaration.addChildDeclaration(irFunction)
    }

    fun generatePropertyDeclaration(ktProperty: KtProperty, containingDeclaration: IrCompoundDeclaration) {
        val sourceLocation = loc(ktProperty)
        val variableDescriptor = getOrFail(BindingContext.VARIABLE, ktProperty) { "unresolved property" }
        val propertyDescriptor = variableDescriptor as? PropertyDescriptor ?: TODO("not a property?")
        if (ktProperty.hasDelegate()) TODO("handle delegated property")
        val initializer = ktProperty.initializer?.let { generateExpressionBody(it) }
        val irProperty = IrSimplePropertyImpl(sourceLocation, containingDeclaration, propertyDescriptor, initializer)
        containingDeclaration.addChildDeclaration(irProperty)

        val irGetter: IrPropertyGetter? = ktProperty.getter?.let { ktGetter ->
            val getterLocation = loc(ktGetter)
            val accessorDescriptor = getOrFail(BindingContext.PROPERTY_ACCESSOR, ktGetter) { "unresolved getter" }
            val getterDescriptor = accessorDescriptor as? PropertyGetterDescriptor ?: TODO("not a getter?")
            val getterBody = generateExpressionBody(ktGetter.bodyExpression ?: TODO("default getter"))
            IrPropertyGetterImpl(getterLocation, irProperty, getterDescriptor, getterBody)
        }
        val irSetter: IrPropertySetter? = ktProperty.setter?.let { ktSetter ->
            val getterLocation = loc(ktSetter)
            val accessorDescriptor = getOrFail(BindingContext.PROPERTY_ACCESSOR, ktSetter) { "unresolved setter" }
            val setterDescriptor = accessorDescriptor as? PropertySetterDescriptor ?: TODO("not a setter?")
            val getterBody = generateExpressionBody(ktSetter.bodyExpression ?: TODO("default setter"))
            IrPropertySetterImpl(getterLocation, irProperty, setterDescriptor, getterBody)
        }
        irProperty.initialize(irGetter, irSetter)
    }

    fun generateExpressionBody(ktBody: KtExpression): IrBody {
        val sourceLocation = loc(ktBody)
        val bodyExpression = irExpressionGenerator.generateExpression(ktBody)

        return if (ktBody is KtBlockExpression)
            bodyExpression
        else
            IrReturnExpressionImpl(sourceLocation, bodyExpression.type, bodyExpression)
    }
}