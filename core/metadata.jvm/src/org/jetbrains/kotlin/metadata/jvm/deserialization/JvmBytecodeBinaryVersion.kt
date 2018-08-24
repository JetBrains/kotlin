/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.metadata.jvm.deserialization

import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion

/**
 * The version of conventions used in bytecode of generated .class files, such as default method naming & signatures,
 * internal member name mangling specifics, property getter/setter names, etc.
 */
class JvmBytecodeBinaryVersion(vararg numbers: Int) : BinaryVersion(*numbers) {
    override fun isCompatible() = this.isCompatibleTo(INSTANCE)

    companion object {
        @JvmField
        val INSTANCE = JvmBytecodeBinaryVersion(1, 0, 3)

        @JvmField
        val INVALID_VERSION = JvmBytecodeBinaryVersion()
    }
}
