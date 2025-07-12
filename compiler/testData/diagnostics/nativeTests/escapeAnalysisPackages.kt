// DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: annotations.kt
package kotlin.native.internal

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class Escapes(val value: Int)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class PointsTo(vararg val value: Int)

// FILE: kotlin_package.kt
package kotlin.collections

import kotlin.native.internal.*

// Functions in kotlin package are checked
@Escapes(0b01)
external fun externalInKotlin(x: Any): Any

<!MISSING_ESCAPE_ANALYSIS_ANNOTATION!>external fun externalInKotlinNoAnnotation(x: Any): Any<!>

// FILE: kotlin_concurrent.kt
package kotlin.concurrent

import kotlin.native.internal.*

// Functions in kotlin.concurrent are NOT checked
<!UNUSED_ESCAPES_ANNOTATION("package outside EA annotation checks")!>@Escapes(0b01)<!>
external fun externalInConcurrent(x: Any): Any

// No error for missing annotation
external fun externalInConcurrentNoAnnotation(x: Any): Any

// FILE: kotlin_native_concurrent.kt
package kotlin.native.concurrent

import kotlin.native.internal.*

// Functions in kotlin.native.concurrent are NOT checked
<!UNUSED_ESCAPES_NOTHING_ANNOTATION("package outside EA annotation checks")!>@Escapes.Nothing<!>
external fun externalInNativeConcurrent(x: Any): Any

// No error for missing annotation
external fun externalInNativeConcurrentNoAnnotation(x: Any): Any

// FILE: other_package.kt
package com.example

import kotlin.native.internal.*

// Functions outside kotlin package are NOT checked
<!UNUSED_ESCAPES_ANNOTATION("package outside EA annotation checks")!>@Escapes(0b01)<!>
external fun externalOutsideKotlin(x: Any): Any

<!UNUSED_POINTS_TO_ANNOTATION("package outside EA annotation checks")!>@PointsTo(0x10)<!>
external fun externalOutsideKotlinPointsTo(x: Any): Any

// No error for missing annotation
external fun externalOutsideKotlinNoAnnotation(x: Any): Any

// FILE: kotlin_text.kt
package kotlin.text

import kotlin.native.internal.*

// Functions in kotlin.text ARE checked (subpackage of kotlin)
@Escapes(0b01)
external fun externalInKotlinText(x: Any): Any

<!MISSING_ESCAPE_ANALYSIS_ANNOTATION!>external fun externalInKotlinTextNoAnnotation(x: Any): Any<!>