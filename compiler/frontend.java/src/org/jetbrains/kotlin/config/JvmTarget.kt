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

import org.jetbrains.kotlin.resolve.JvmTarget
import org.jetbrains.kotlin.resolve.JvmTarget.*
import org.jetbrains.org.objectweb.asm.Opcodes

fun getDescription(bytecodeVersion: Int): String {
    val platformDescription = values().find { it.bytecodeVersion == bytecodeVersion }?.description ?: when (bytecodeVersion) {
        Opcodes.V1_7 -> "1.7"
        else -> null
    }

    return if (platformDescription != null) "JVM target $platformDescription"
    else "JVM bytecode version $bytecodeVersion"
}

val JvmTarget.bytecodeVersion: Int
    get() = when (this) {
        JVM_1_6 -> Opcodes.V1_6
        JVM_1_8 ->
            when {
                java.lang.Boolean.valueOf(System.getProperty("kotlin.test.substitute.bytecode.1.8.to.10")) -> Opcodes.V9 + 1
                java.lang.Boolean.valueOf(System.getProperty("kotlin.test.substitute.bytecode.1.8.to.1.9")) -> Opcodes.V9
                else -> Opcodes.V1_8
            }
        JVM_9 -> Opcodes.V9
        JVM_10 -> Opcodes.V9 + 1
        JVM_11 -> Opcodes.V9 + 2
        JVM_12 -> Opcodes.V9 + 3
    }