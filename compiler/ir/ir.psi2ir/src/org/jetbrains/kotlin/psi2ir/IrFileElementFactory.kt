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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class IrFileElementFactory private constructor(
        val fileEntry: PsiSourceManager.PsiFileEntry,
        val irFileImpl: IrFileImpl,
        val containingDeclaration: IrCompoundDeclaration
) {
    fun createChild(containingDeclaration: IrCompoundDeclaration) =
            IrFileElementFactory(fileEntry, irFileImpl, containingDeclaration)

    private fun <D : IrMemberDeclaration> D.addToContainer(): D =
            apply { containingDeclaration.addChildDeclaration(this) }

    fun createFunction(ktFunction: KtFunction, functionDescriptor: FunctionDescriptor, body: IrBody): IrFunction =
            IrFunctionImpl(ktFunction.startOffset, ktFunction.endOffset,
                           IrDeclarationOriginKind.DEFINED, functionDescriptor, body)
                    .addToContainer()

    fun createSimpleProperty(ktProperty: KtProperty, propertyDescriptor: PropertyDescriptor, valueInitializer: IrBody?): IrSimpleProperty =
            IrSimplePropertyImpl(ktProperty.startOffset, ktProperty.endOffset,
                                 IrDeclarationOriginKind.DEFINED, propertyDescriptor, valueInitializer)
                    .addToContainer()

    fun createPropertyGetter(
            ktPropertyAccessor: KtPropertyAccessor,
            irProperty: IrProperty,
            getterDescriptor: PropertyGetterDescriptor,
            getterBody: IrBody
    ): IrPropertyGetter =
            IrPropertyGetterImpl(ktPropertyAccessor.startOffset, ktPropertyAccessor.endOffset,
                                 IrDeclarationOriginKind.DEFINED, getterDescriptor, getterBody)
                    .apply { irProperty.getter = this }
                    .addToContainer()

    fun createPropertySetter(
            ktPropertyAccessor: KtPropertyAccessor,
            irProperty: IrProperty,
            setterDescriptor: PropertySetterDescriptor,
            setterBody: IrBody
    ) : IrPropertySetter =
            IrPropertySetterImpl(ktPropertyAccessor.startOffset, ktPropertyAccessor.endOffset,
                                 IrDeclarationOriginKind.DEFINED, setterDescriptor, setterBody)
                    .apply { irProperty.setter = this }
                    .addToContainer()

    companion object {
        fun create(irModule: IrModuleImpl, sourceManager: PsiSourceManager, ktFile: KtFile, descriptor: PackageFragmentDescriptor): IrFileElementFactory {
            val fileEntry = sourceManager.getOrCreateFileEntry(ktFile)
            val fileName = fileEntry.getRecognizableName()
            val irFile = IrFileImpl(fileEntry, fileName, descriptor)
            sourceManager.putFileEntry(irFile, fileEntry)
            irModule.addFile(irFile)
            return IrFileElementFactory(fileEntry, irFile, irFile)
        }
    }
}