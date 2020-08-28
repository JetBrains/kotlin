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

package org.jetbrains.kotlin.ir.expressions.persistent

import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.persistent.PersistentIrBodyBase
import org.jetbrains.kotlin.ir.declarations.persistent.PersistentIrFactory
import org.jetbrains.kotlin.ir.declarations.persistent.carriers.Carrier
import org.jetbrains.kotlin.ir.declarations.stageController
import org.jetbrains.kotlin.ir.expressions.IrBlockBody

internal class PersistentIrBlockBody(
    override val startOffset: Int,
    override val endOffset: Int,
    override var initializer: (PersistentIrBlockBody.() -> Unit)? = null
) : IrBlockBody(), PersistentIrBodyBase<PersistentIrBlockBody> {
    override var lastModified: Int = stageController.currentStage
    override var loweredUpTo: Int = stageController.currentStage
    override var values: Array<Carrier>? = null
    override val createdOn: Int = stageController.currentStage

    override var containerField: IrDeclaration? = null

    constructor(startOffset: Int, endOffset: Int, statements: List<IrStatement>) : this(startOffset, endOffset) {
        statementsField.addAll(statements)
    }

    private var statementsField: MutableList<IrStatement> = ArrayList()

    override val statements: MutableList<IrStatement>
        get() = checkEnabled { statementsField }

    override val factory: IrFactory
        get() = PersistentIrFactory
}
