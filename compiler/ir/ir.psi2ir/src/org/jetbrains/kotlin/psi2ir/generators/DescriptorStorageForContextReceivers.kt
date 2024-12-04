/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue

internal class DescriptorStorageForContextReceivers {
    private val syntheticFieldStorage: MutableMap<ReceiverValue, IrField> = mutableMapOf()

    fun put(receiverValue: ReceiverValue, irField: IrField) {
        syntheticFieldStorage[receiverValue] = irField
    }

    fun getSyntheticField(receiverValue: ReceiverValue) =
        syntheticFieldStorage[receiverValue] ?: error("No synthetic field for receiver value $receiverValue")

    fun addAllFrom(storage: DescriptorStorageForContextReceivers) {
        syntheticFieldStorage.putAll(storage.syntheticFieldStorage)
    }
}
