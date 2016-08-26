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

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext

class ClassGenerator(val declarationGenerator: DeclarationGenerator) : Generator {
    override val context: GeneratorContext get() = declarationGenerator.context

    fun generateClass(ktClassOrObject: KtClassOrObject): IrClass {
        val descriptor = getOrFail(BindingContext.CLASS, ktClassOrObject)
        val irClass = IrClassImpl(ktClassOrObject.startOffset, ktClassOrObject.endOffset, IrDeclarationOrigin.DEFINED, descriptor)

        generatePropertiesDeclaredInPrimaryConstructor(irClass, ktClassOrObject)
        generateMembersDeclaredInClassBody(irClass, ktClassOrObject)

        return irClass
    }

    private fun generatePropertiesDeclaredInPrimaryConstructor(irClass: IrClassImpl, ktClassOrObject: KtClassOrObject) {
        ktClassOrObject.getPrimaryConstructor()?.let { ktPrimaryConstructor ->
            for (ktParameter in ktPrimaryConstructor.valueParameters) {
                if (ktParameter.hasValOrVar()) {
                    irClass.addMember(generatePropertyForPrimaryConstructorParameter(ktParameter))
                }
            }
        }
    }

    private fun generateMembersDeclaredInClassBody(irClass: IrClassImpl, ktClassOrObject: KtClassOrObject) {
        ktClassOrObject.getBody()?.let { ktClassBody ->
            for (ktDeclaration in ktClassBody.declarations) {
                val irMember = declarationGenerator.generateMemberDeclaration(ktDeclaration)
                irClass.addMember(irMember)
                if (irMember is IrProperty) {
                    irMember.getter?.let { irClass.addMember(it) }
                    irMember.setter?.let { irClass.addMember(it) }
                }
            }
        }
    }

    private fun generatePropertyForPrimaryConstructorParameter(ktParameter: KtParameter): IrDeclaration {
        val propertyDescriptor = getOrFail(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, ktParameter)
        return IrSimplePropertyImpl(ktParameter.startOffset, ktParameter.endOffset, IrDeclarationOrigin.DEFINED, propertyDescriptor)
    }
}
