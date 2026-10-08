/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.buildtools.api.jvm.AccessibleClassSnapshot
import org.jetbrains.kotlin.buildtools.api.jvm.InaccessibleClassSnapshot

@Serializable
internal class AccessibleClassSnapshotImpl(override val classAbiHash: Long) : AccessibleClassSnapshot {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AccessibleClassSnapshot) return false
        return classAbiHash == other.classAbiHash
    }

    override fun hashCode(): Int = classAbiHash.hashCode()

    override fun toString(): String = "AccessibleClassSnapshot(classAbiHash=$classAbiHash)"
}

@Serializable
internal object InaccessibleClassSnapshotImpl : InaccessibleClassSnapshot {
    override fun equals(other: Any?): Boolean = other is InaccessibleClassSnapshot
    override fun hashCode(): Int = 0
    override fun toString(): String = "InaccessibleClassSnapshot"
}
