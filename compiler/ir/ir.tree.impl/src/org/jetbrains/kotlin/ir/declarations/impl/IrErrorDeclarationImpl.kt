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

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrErrorDeclaration
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall

@OptIn(ObsoleteDescriptorBasedAPI::class)
class IrErrorDeclarationImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    private val _descriptor: DeclarationDescriptor?
) : IrErrorDeclaration() {
    override val descriptor: DeclarationDescriptor
        get() = _descriptor ?: this.toIrBasedDescriptor()

    override var origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED

    override val factory: IrFactory
        get() = IrFactoryImpl

    override lateinit var parent: IrDeclarationParent
    override var annotations: List<IrConstructorCall> = emptyList()
}
