/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.config

import org.jetbrains.org.objectweb.asm.Opcodes

enum class JvmTarget(override val description: String) : TargetPlatformVersion {
    JVM_1_6("1.6"),
    JVM_1_8("1.8"),
    ;

    val bytecodeVersion: Int by lazy {
        when (this) {
            JVM_1_6 -> Opcodes.V1_6
            JVM_1_8 -> Opcodes.V1_8
        }
    }

    companion object {
        @JvmField
        val DEFAULT = JVM_1_6

        @JvmStatic
        fun fromString(string: String) = values().find { it.description == string }

        fun getDescription(bytecodeVersion: Int): String {
            val platformDescription = values().find { it.bytecodeVersion == bytecodeVersion }?.description ?:
                   when (bytecodeVersion) {
                       Opcodes.V1_7 -> "1.7"
                       Opcodes.V1_8 + 1 -> "1.9"
                       else -> null
                   }

            return if (platformDescription != null) "JVM target $platformDescription"
                    else "JVM bytecode version $bytecodeVersion"
        }
    }
}
