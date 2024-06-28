// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt
expect annotation class TypealiasToKotlinPkg

internal expect annotation class TypealiasToInternalPkg

expect annotation class TypealiasToAnnotationPkg

expect annotation class TypealiasToPlatformPkg

expect enum class TypealiasNotToAnnotation

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual typealias TypealiasToKotlinPkg = <!ACTUAL_TYPEALIAS_TO_SPECIAL_ANNOTATION!>kotlin.Deprecated<!>

@Suppress(<!ERROR_SUPPRESSION!>"INVISIBLE_REFERENCE"<!>)
internal actual typealias TypealiasToInternalPkg = <!ACTUAL_TYPEALIAS_TO_SPECIAL_ANNOTATION!>kotlin.internal.RequireKotlin<!>

actual typealias TypealiasToAnnotationPkg = <!ACTUAL_TYPEALIAS_TO_SPECIAL_ANNOTATION!>kotlin.annotation.Target<!>

actual typealias TypealiasToPlatformPkg = kotlin.jvm.Synchronized

typealias NonActualTypealias = kotlin.Deprecated

actual typealias TypealiasNotToAnnotation = kotlin.DeprecationLevel
