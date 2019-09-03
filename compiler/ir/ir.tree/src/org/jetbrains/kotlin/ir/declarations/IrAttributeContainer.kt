/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.ir.IrElement

interface IrAttributeContainer : IrElement {
    /**
     * An object which can be used as a key in the map in the backend-specific storage. If a brand new IR element is created, this property
     * should be equal to that element itself. If an element is copied from some other element, its id should be copied as well.
     *
     * Note that an [attributeOwnerId] of any element must be an element, whose [attributeOwnerId] is itself. In other words,
     * it shouldn't be needed to call this more than once to find the original attribute owner.
     */
    var attributeOwnerId: IrAttributeContainer
}

fun <D : IrAttributeContainer> D.copyAttributes(other: IrAttributeContainer?): D = apply {
    if (other != null) {
        attributeOwnerId = other.attributeOwnerId
    }
}
