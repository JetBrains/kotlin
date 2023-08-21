// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt
package kotlin

@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
expect annotation class OptionalExpectationOnExpectOnly

@RequiresOptIn
annotation class MyOptIn

@SinceKotlin("1.8")
@Deprecated(message = "Some text")
@DeprecatedSinceKotlin("1.8")
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@MyOptIn
@WasExperimental(MyOptIn::class)
@kotlin.internal.RequireKotlin(version = "1.8")
@OptIn(MyOptIn::class)
expect fun skippedAnnotationsOnExpectOnly()

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
package kotlin

@OptIn(ExperimentalMultiplatform::class)
actual annotation class OptionalExpectationOnExpectOnly

actual fun skippedAnnotationsOnExpectOnly() {}