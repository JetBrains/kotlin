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

import org.jetbrains.kotlin.platform.TargetPlatformVersion
import org.jetbrains.org.objectweb.asm.Opcodes

enum class JvmTarget(
    override val description: String,
    val majorVersion: Int,
) : TargetPlatformVersion {
    JVM_1_6("1.6", Opcodes.V1_6),
    JVM_1_8("1.8", Opcodes.V1_8),
    JVM_9("9", Opcodes.V9),
    JVM_10("10", Opcodes.V10),
    JVM_11("11", Opcodes.V11),
    JVM_12("12", Opcodes.V12),
    JVM_13("13", Opcodes.V13),
    JVM_14("14", Opcodes.V14),
    JVM_15("15", Opcodes.V15),
    JVM_16("16", Opcodes.V16),
    JVM_17("17", Opcodes.V16 + 1),
    JVM_18("18", Opcodes.V16 + 2),
    JVM_19("19", Opcodes.V16 + 3),
    JVM_20("20", Opcodes.V16 + 4),
    ;

    override fun toString() = description

    companion object {
        @JvmField
        val DEFAULT = JVM_1_8

        @JvmStatic
        fun fromString(string: String) = values().find { it.description == string }

        fun getDescription(majorVersion: Int): String {
            val platformDescription = values().find { it.majorVersion == majorVersion }?.description ?: when (majorVersion) {
                Opcodes.V1_7 -> "1.7"
                else -> null
            }

            return if (platformDescription != null) "JVM target $platformDescription"
            else "JVM bytecode version $majorVersion"
        }

        fun supportedValues(): List<JvmTarget> =
            values().asList() - JVM_1_6

        const val SUPPORTED_VERSIONS_DESCRIPTION =
            "1.8, 9, 10, ..., 20"

        init {
            check(SUPPORTED_VERSIONS_DESCRIPTION == "1.8, 9, 10, ..., ${values().last().description}") {
                "Please update the value of the constant JvmTarget.SUPPORTED_VERSIONS_DESCRIPTION."
            }
        }
    }
}
