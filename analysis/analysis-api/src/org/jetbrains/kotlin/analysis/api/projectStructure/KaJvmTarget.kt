/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.projectStructure

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.org.objectweb.asm.Opcodes

/**
 * Represents a JVM bytecode target version for compilation.
 *
 * Each instance corresponds to a specific Java SE release and its bytecode version.
 * Use the companion object constants (e.g., [JVM_1_8], [JVM_17], [JVM_21]) to specify the target.
 *
 * @property name The human-readable version string (e.g., `"1.8"`, `"17"`).
 * @property bytecodeMajorVersion The class file major version number (e.g., `52` for Java 8, `61` for Java 17).
 */
@KaExperimentalApi
public class KaJvmTarget private constructor(public val name: String, public val bytecodeMajorVersion: Int) {
    @KaExperimentalApi
    public companion object {
        /** Java 8: lambdas, default methods, streams, `java.time`. */
        public val JVM_1_8: KaJvmTarget = KaJvmTarget("1.8", Opcodes.V1_8)

        /** Java 9: module system (JPMS), private interface methods, `VarHandle`. */
        public val JVM_9: KaJvmTarget = KaJvmTarget("9", Opcodes.V9)

        /** Java 10: local-variable type inference (`var`). */
        public val JVM_10: KaJvmTarget = KaJvmTarget("10", Opcodes.V10)

        /** Java 11: nest-based access control, dynamic constants (`CONSTANT_Dynamic`). */
        public val JVM_11: KaJvmTarget = KaJvmTarget("11", Opcodes.V11)

        /** Java 12: switch expressions (preview). */
        public val JVM_12: KaJvmTarget = KaJvmTarget("12", Opcodes.V12)

        /** Java 13: text blocks (preview). */
        public val JVM_13: KaJvmTarget = KaJvmTarget("13", Opcodes.V13)

        /** Java 14: records (preview), `instanceof` pattern matching (preview). */
        public val JVM_14: KaJvmTarget = KaJvmTarget("14", Opcodes.V14)

        /** Java 15: sealed classes (preview), hidden classes. */
        public val JVM_15: KaJvmTarget = KaJvmTarget("15", Opcodes.V15)

        /** Java 16: records (final), `instanceof` pattern matching (final). */
        public val JVM_16: KaJvmTarget = KaJvmTarget("16", Opcodes.V16)

        /** Java 17 (LTS): sealed classes (final), enhanced pseudo-random number generators. */
        public val JVM_17: KaJvmTarget = KaJvmTarget("17", Opcodes.V17)

        /** Java 18: UTF-8 by default, simple web server. */
        public val JVM_18: KaJvmTarget = KaJvmTarget("18", Opcodes.V18)

        /** Java 19: virtual threads (preview), structured concurrency (incubator). */
        public val JVM_19: KaJvmTarget = KaJvmTarget("19", Opcodes.V19)

        /** Java 20: scoped values (incubator), record patterns (preview). */
        public val JVM_20: KaJvmTarget = KaJvmTarget("20", Opcodes.V20)

        /** Java 21 (LTS): virtual threads (final), record patterns (final), sequenced collections. */
        public val JVM_21: KaJvmTarget = KaJvmTarget("21", Opcodes.V21)

        /** Java 22: unnamed variables, flexible constructor bodies (preview). */
        public val JVM_22: KaJvmTarget = KaJvmTarget("22", Opcodes.V22)

        /** Java 23: primitive types in patterns (preview). */
        public val JVM_23: KaJvmTarget = KaJvmTarget("23", Opcodes.V22 + 1)

        /** Java 24: stream gatherers (final), flexible constructor bodies. */
        public val JVM_24: KaJvmTarget = KaJvmTarget("24", Opcodes.V22 + 2)

        /** Java 25 (LTS): stable value types (preview), compact object headers (preview). */
        public val JVM_25: KaJvmTarget = KaJvmTarget("25", Opcodes.V22 + 3)

        /** Java 26; primitive types in patterns (stable). */
        public val JVM_26: KaJvmTarget = KaJvmTarget("26", Opcodes.V22 + 4)

        /** Java 27. */
        public val JVM_27: KaJvmTarget = KaJvmTarget("27", Opcodes.V22 + 5)
    }

    override fun toString(): String {
        return "KaJvmTarget($name)"
    }
}