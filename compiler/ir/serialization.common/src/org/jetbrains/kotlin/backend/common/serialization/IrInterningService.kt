/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.name.Name
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet

/**
 * The interface provide an API for interning [String] and [Name] values
 * to save memory by eliminating duplicates of instances of those classes
 */
class IrInterningService {
    /**
     * We use here an open-addressing map, because it consumes at least twice lesser memory than with bucket-based implementation:
     * - Open-addressing (cost per entry): ref to key + ref to value
     * - Bucket-based (cost per entry): hash code + ref to key + ref to value + ref to the next node + class header with memory alignment
     */
    private val strings by lazy { ObjectOpenHashSet<String>() }
    private val names by lazy { ObjectOpenHashSet<Name>() }

    fun string(string: String): String {
        return strings.addOrGet(string)
    }

    fun name(name: Name): Name {
        return names.addOrGet(name)
    }

    fun clear() {
        strings.clear()
        names.clear()
    }
}