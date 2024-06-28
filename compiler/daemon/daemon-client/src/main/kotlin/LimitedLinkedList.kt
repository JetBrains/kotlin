/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.client

import java.util.*


internal class LimitedLinkedList<E>(private val limit: Int) : LinkedList<E>() {
    init {
        require(limit > 0) {
            "The limit shall be > 0. Other values does not make any sense"
        }
    }

    override fun add(element: E): Boolean {
        val added = super.add(element)
        while (added && size > limit) {
            super.remove()
        }
        return added
    }
}