/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir

class BirForest {
    private val rootElements = mutableListOf<BirElementBase>()

    internal fun elementAttached(element: BirElementBase) {
        element.accept {
            attachElement(it as BirElementBase)
            it.walkIntoChildren()
        }
    }

    private fun attachElement(element: BirElementBase) {
        element.owner = this
    }

    internal fun rootElementAttached(element: BirElementBase) {
        elementAttached(element)
        rootElements += element
    }

    internal fun elementDetached(element: BirElementBase) {
        element.accept {
            detachElement(it as BirElementBase)
            it.walkIntoChildren()
        }
    }

    private fun detachElement(element: BirElementBase) {
        element.owner = null
    }
}