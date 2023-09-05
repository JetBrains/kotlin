// FIR_IDENTICAL
// LANGUAGE: -MultiplatformRestrictions
// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt
@file:OptIn(ExperimentalMultiplatform::class)

expect annotation class ActualOnly

@RequiresOptIn
expect annotation class Both

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
@RequiresOptIn
actual annotation class ActualOnly

@RequiresOptIn
actual annotation class Both
