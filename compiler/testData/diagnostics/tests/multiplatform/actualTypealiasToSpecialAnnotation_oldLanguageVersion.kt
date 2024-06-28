// LANGUAGE: -MultiplatformRestrictions
// FIR_IDENTICAL
// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt
expect annotation class TypealiasToKotlinPkg

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual typealias TypealiasToKotlinPkg = kotlin.Deprecated

