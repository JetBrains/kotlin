/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.projectStructure

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi

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
        @JvmField
        public val JVM_1_8: KaJvmTarget = KaJvmTarget("1.8", 52)

        /** Java 9: module system (JPMS), private interface methods, `VarHandle`. */
        @JvmField
        public val JVM_9: KaJvmTarget = KaJvmTarget("9", 53)

        /** Java 10: local-variable type inference (`var`). */
        @JvmField
        public val JVM_10: KaJvmTarget = KaJvmTarget("10", 54)

        /** Java 11: nest-based access control, dynamic constants (`CONSTANT_Dynamic`). */
        @JvmField
        public val JVM_11: KaJvmTarget = KaJvmTarget("11", 55)

        /** Java 12: switch expressions (preview). */
        @JvmField
        public val JVM_12: KaJvmTarget = KaJvmTarget("12", 56)

        /** Java 13: text blocks (preview). */
        @JvmField
        public val JVM_13: KaJvmTarget = KaJvmTarget("13", 57)

        /** Java 14: records (preview), `instanceof` pattern matching (preview). */
        @JvmField
        public val JVM_14: KaJvmTarget = KaJvmTarget("14", 58)

        /** Java 15: sealed classes (preview), hidden classes. */
        @JvmField
        public val JVM_15: KaJvmTarget = KaJvmTarget("15", 59)

        /** Java 16: records (final), `instanceof` pattern matching (final). */
        @JvmField
        public val JVM_16: KaJvmTarget = KaJvmTarget("16", 60)

        /** Java 17 (LTS): sealed classes (final), enhanced pseudo-random number generators. */
        @JvmField
        public val JVM_17: KaJvmTarget = KaJvmTarget("17", 61)

        /** Java 18: UTF-8 by default, simple web server. */
        @JvmField
        public val JVM_18: KaJvmTarget = KaJvmTarget("18", 62)

        /** Java 19: virtual threads (preview), structured concurrency (incubator). */
        @JvmField
        public val JVM_19: KaJvmTarget = KaJvmTarget("19", 63)

        /** Java 20: scoped values (incubator), record patterns (preview). */
        @JvmField
        public val JVM_20: KaJvmTarget = KaJvmTarget("20", 64)

        /** Java 21 (LTS): virtual threads (final), record patterns (final), sequenced collections. */
        @JvmField
        public val JVM_21: KaJvmTarget = KaJvmTarget("21", 65)

        /** Java 22: unnamed variables, flexible constructor bodies (preview). */
        @JvmField
        public val JVM_22: KaJvmTarget = KaJvmTarget("22", 66)

        /** Java 23: primitive types in patterns (preview). */
        @JvmField
        public val JVM_23: KaJvmTarget = KaJvmTarget("23", 67)

        /** Java 24: stream gatherers (final), flexible constructor bodies. */
        @JvmField
        public val JVM_24: KaJvmTarget = KaJvmTarget("24", 68)

        /** Java 25 (LTS): stable value types (preview), compact object headers (preview). */
        @JvmField
        public val JVM_25: KaJvmTarget = KaJvmTarget("25", 69)

        /** Java 26; primitive types in patterns (stable). */
        @JvmField
        public val JVM_26: KaJvmTarget = KaJvmTarget("26", 70)

        /** Java 27. */
        @JvmField
        public val JVM_27: KaJvmTarget = KaJvmTarget("27", 71)

        /** Java 28. */
        @JvmField
        public val JVM_28: KaJvmTarget = KaJvmTarget("28", 72)

        /**
         * List of all targets provided by [KaJvmTarget].
         */
        @JvmField
        public val ALL_TARGETS: List<KaJvmTarget> = listOf(
            JVM_1_8,
            JVM_9,
            JVM_10,
            JVM_11,
            JVM_12,
            JVM_13,
            JVM_14,
            JVM_15,
            JVM_16,
            JVM_17,
            JVM_18,
            JVM_19,
            JVM_20,
            JVM_21,
            JVM_22,
            JVM_23,
            JVM_24,
            JVM_25,
            JVM_26,
            JVM_27,
            JVM_28,
        )
    }

    override fun toString(): String {
        return "KaJvmTarget($name)"
    }
}
