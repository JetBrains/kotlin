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

package org.jetbrains.kotlin.ir.declarations.persistent

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.persistent.carriers.AnonymousInitializerCarrier
import org.jetbrains.kotlin.ir.declarations.persistent.carriers.Carrier
import org.jetbrains.kotlin.ir.declarations.stageController
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrAnonymousInitializerSymbol

internal class PersistentIrAnonymousInitializer(
    override val startOffset: Int,
    override val endOffset: Int,
    origin: IrDeclarationOrigin,
    override val symbol: IrAnonymousInitializerSymbol,
    override val isStatic: Boolean = false
) : IrAnonymousInitializer(),
    PersistentIrDeclarationBase<AnonymousInitializerCarrier>,
    AnonymousInitializerCarrier {

    init {
        symbol.bind(this)
    }

    override var lastModified: Int = stageController.currentStage
    override var loweredUpTo: Int = stageController.currentStage
    override var values: Array<Carrier>? = null
    override val createdOn: Int = stageController.currentStage

    override var parentField: IrDeclarationParent? = null
    override var originField: IrDeclarationOrigin = origin
    override var removedOn: Int = Int.MAX_VALUE
    override var annotationsField: List<IrConstructorCall> = emptyList()

    @ObsoleteDescriptorBasedAPI
    override val descriptor: ClassDescriptor
        get() = symbol.descriptor

    override var bodyField: IrBlockBody? = null

    override var body: IrBlockBody
        get() = getCarrier().bodyField!!
        set(v) {
            if (getCarrier().bodyField !== v) {
                if (v is PersistentIrBodyBase<*>) {
                    v.container = this
                }
                setCarrier().bodyField = v
            }
        }
}
