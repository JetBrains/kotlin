// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// ALLOW_KOTLIN_PACKAGE
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
@Suppress(<!ERROR_SUPPRESSION!>"INVISIBLE_REFERENCE"<!>, "INVISIBLE_MEMBER")
@MyOptIn
@WasExperimental(MyOptIn::class)
@kotlin.internal.RequireKotlin(version = "1.8")
@OptIn(MyOptIn::class)
expect fun skippedAnnotationsOnExpectOnly()

@OptIn(ExperimentalMultiplatform::class)
@kotlin.jvm.ImplicitlyActualizedByJvmDeclaration
expect class ImplicitlyActualizedByJvmDeclarationOnExpectOnly

@SubclassOptInRequired(MyOptIn::class)
expect open class SubclassOptInRequiredOnExpectOnly

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
package kotlin

@OptIn(ExperimentalMultiplatform::class)
actual annotation class OptionalExpectationOnExpectOnly

actual fun skippedAnnotationsOnExpectOnly() {}

actual class ImplicitlyActualizedByJvmDeclarationOnExpectOnly

actual open class SubclassOptInRequiredOnExpectOnly
