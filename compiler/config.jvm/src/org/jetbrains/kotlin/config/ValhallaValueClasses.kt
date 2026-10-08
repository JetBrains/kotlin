/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

import org.jetbrains.kotlin.descriptors.FullValueClassRepresentation
import org.jetbrains.kotlin.descriptors.InlineClassRepresentation
import org.jetbrains.kotlin.descriptors.ValueClassRepresentation

fun ValueClassRepresentation<*>?.isKotlinValhallaValueClass(languageVersionSettings: LanguageVersionSettings): Boolean = when (this) {
    is InlineClassRepresentation<*>, is FullValueClassRepresentation<*> -> languageVersionSettings.isValhallaSupportEnabled()
    null -> false
}

/**
 * Whether value classes (JEP 401) are available with [jvmTarget]: they are a preview feature since JVM 28, so they also need JVM preview
 * features to be enabled ([isJvmPreviewEnabled]).
 */
fun isJvmTargetValhallaCompatible(jvmTarget: JvmTarget, isJvmPreviewEnabled: Boolean): Boolean =
    isJvmPreviewEnabled && jvmTarget >= JvmTarget.JVM_28

/**
 * Whether every value class is compiled to and behaves as a Project Valhalla value class, selected via `-Xvalhalla-value-classes`.
 *
 * Requires JVM target 28 or later and the `-Xjvm-enable-preview` flag. When disabled, no declaration is compiled as a Valhalla value
 * class and regular JVM bytecode is generated.
 */
fun LanguageVersionSettings.isValhallaSupportEnabled(): Boolean = getFlag(JvmAnalysisFlags.valhallaValueClasses)

/**
 * The internal names of the JDK classes that are value classes when JVM preview features are enabled, the abstract `Number`, `Record`
 * and `ChronoLocalDateImpl` among them. Kotlin reads the non-preview class files of the JDK, where they are identity classes.
 * `JdkValueClassesTest` compares this set with the value classes of the JDK provided via `JDK_VALHALLA`.
 */
val JDK_VALUE_CLASSES: Set<String> = setOf(
    "java/lang/Boolean", "java/lang/Byte", "java/lang/Character", "java/lang/Short",
    "java/lang/Integer", "java/lang/Long", "java/lang/Float", "java/lang/Double",
    "java/lang/Number", "java/lang/Record",
    "java/time/Duration", "java/time/Instant", "java/time/LocalDate", "java/time/LocalDateTime",
    "java/time/LocalTime", "java/time/MonthDay", "java/time/OffsetDateTime", "java/time/OffsetTime",
    "java/time/Period", "java/time/Year", "java/time/YearMonth", "java/time/ZonedDateTime",
    "java/time/chrono/ChronoLocalDateImpl", "java/time/chrono/HijrahDate", "java/time/chrono/JapaneseDate",
    "java/time/chrono/MinguoDate", "java/time/chrono/ThaiBuddhistDate",
    "java/util/Optional", "java/util/OptionalDouble", "java/util/OptionalInt", "java/util/OptionalLong",
)
