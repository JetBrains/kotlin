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
import org.jetbrains.kotlin.ir.expressions.IrDummyBody
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.util.slicedMap.ReadOnlySlice

interface IrGenerator {
    val context: IrGeneratorContext

    operator fun <K, V : Any> ReadOnlySlice<K, V>.get(key: K): V? =
            context.bindingContext[this, key]
}


interface IrDeclarationGenerator : IrGenerator {
    val irDeclaration: IrDeclaration
    val parent: IrDeclarationGenerator?
}

class IrModuleGenerator(override val context: IrGeneratorContext) : IrDeclarationGenerator {
    override val irDeclaration: IrModule get() = context.irModule
    override val parent: IrDeclarationGenerator? get() = null

    fun generateModuleContent() {
        for (ktFile in context.inputFiles) {
            val packageFragmentDescriptor = BindingContext.FILE_TO_PACKAGE_FRAGMENT[ktFile] ?: TODO("Handle unresolved code")
            val irFile = context.irElementFactory.createIrFile(ktFile, packageFragmentDescriptor)
            irDeclaration.addFile(irFile)
            val generator = IrFileGenerator(context, ktFile, irFile, this)
            generator.generateFileContent()
        }
    }
}

abstract class IrDeclarationGeneratorBase(
        override val context: IrGeneratorContext,
        override val irDeclaration: IrDeclaration,
        override val parent: IrDeclarationGenerator,
        val file: PsiSourceManager.PsiFileEntry
) : IrDeclarationGenerator {
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

    fun generateFunctionDeclaration(ktNamedFunction: KtNamedFunction, containingDeclaration: IrCompoundDeclaration) {
        val sourceLocation = file.getSourceLocationForElement(ktNamedFunction)
        val functionDescriptor = BindingContext.FUNCTION[ktNamedFunction] ?: TODO("unresolved fun")
        val body = generateExpressionBody(ktNamedFunction.bodyExpression ?: TODO("function without body expression"))
        val irFunction = IrFunctionImpl(sourceLocation, containingDeclaration, functionDescriptor, body)
        containingDeclaration.addChildDeclaration(irFunction)
    }

    fun generatePropertyDeclaration(ktProperty: KtProperty, containingDeclaration: IrCompoundDeclaration) {
        val sourceLocation = file.getSourceLocationForElement(ktProperty)
        val variableDescriptor = BindingContext.VARIABLE[ktProperty] ?: TODO("unresolved property")
        val propertyDescriptor = variableDescriptor as? PropertyDescriptor ?: TODO("not a property?")
        if (ktProperty.hasDelegate()) TODO("handle delegated property")
        val initializer = ktProperty.initializer?.let { generateExpressionBody(it) }
        val irProperty = IrSimplePropertyImpl(sourceLocation, containingDeclaration, propertyDescriptor, initializer)
        containingDeclaration.addChildDeclaration(irProperty)

        val irGetter: IrPropertyGetter? = ktProperty.getter?.let { ktGetter ->
            val getterLocation = file.getSourceLocationForElement(ktGetter)
            val accessorDescriptor = BindingContext.PROPERTY_ACCESSOR[ktGetter] ?: TODO("unresolved getter")
            val getterDescriptor = accessorDescriptor as? PropertyGetterDescriptor ?: TODO("not a getter?")
            val getterBody = generateExpressionBody(ktGetter.bodyExpression ?: TODO("default getter"))
            IrPropertyGetterImpl(getterLocation, irProperty, getterDescriptor, getterBody)
        }
        val irSetter: IrPropertySetter? = ktProperty.setter?.let { ktSetter ->
            val getterLocation = file.getSourceLocationForElement(ktSetter)
            val accessorDescriptor = BindingContext.PROPERTY_ACCESSOR[ktSetter] ?: TODO("unresolved setter")
            val setterDescriptor = accessorDescriptor as? PropertySetterDescriptor ?: TODO("not a setter?")
            val getterBody = generateExpressionBody(ktSetter.bodyExpression ?: TODO("default setter"))
            IrPropertySetterImpl(getterLocation, irProperty, setterDescriptor, getterBody)
        }
        irProperty.initialize(irGetter, irSetter)
    }

    fun generateExpressionBody(ktExpression: KtExpression): IrBody {
        val sourceLocation = file.getSourceLocationForElement(ktExpression)
        // TODO
        return IrDummyBody(sourceLocation)
    }
}

class IrFileGenerator(
        context: IrGeneratorContext,
        val ktFile: KtFile,
        override val irDeclaration: IrFile,
        override val parent: IrModuleGenerator
) : IrDeclarationGeneratorBase(context, irDeclaration, parent, irDeclaration.fileEntry as PsiSourceManager.PsiFileEntry) {
    fun generateFileContent() {
        generateAnnotationEntries(ktFile.annotationEntries)

        for (topLevelDeclaration in ktFile.declarations) {
            generateMemberDeclaration(topLevelDeclaration, irDeclaration)
        }
    }
}