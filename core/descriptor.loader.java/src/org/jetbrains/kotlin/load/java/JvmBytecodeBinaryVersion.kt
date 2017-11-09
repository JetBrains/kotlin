/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.load.java

import org.jetbrains.kotlin.serialization.deserialization.BinaryVersion

/**
 * The version of conventions used in bytecode of generated .class files, such as default method naming & signatures,
 * internal member name mangling specifics, property getter/setter names, etc.
 */
class JvmBytecodeBinaryVersion(vararg numbers: Int) : BinaryVersion(*numbers) {
    override fun isCompatible() = this.isCompatibleTo(INSTANCE)

    companion object {
        @JvmField
        val INSTANCE = JvmBytecodeBinaryVersion(1, 0, 2)

        @JvmField
        val INVALID_VERSION = JvmBytecodeBinaryVersion()
    }
}
