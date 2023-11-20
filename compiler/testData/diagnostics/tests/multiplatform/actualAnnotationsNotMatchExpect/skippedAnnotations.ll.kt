// LL_FIR_DIVERGENCE
// UNRESOLVED_REFERENCE on MyOptIn is due to bug KT-61757
// LL_FIR_DIVERGENCE
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
@Suppress(<!ERROR_SUPPRESSION!>"INVISIBLE_REFERENCE"<!>, "INVISIBLE_MEMBER")
@<!UNRESOLVED_REFERENCE!>MyOptIn<!>
@WasExperimental(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!><!UNRESOLVED_REFERENCE!>MyOptIn<!>::class<!>)
@kotlin.internal.RequireKotlin(version = "1.8")
@OptIn(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!><!UNRESOLVED_REFERENCE!>MyOptIn<!>::class<!>)
expect fun skippedAnnotationsOnExpectOnly()

@OptIn(ExperimentalMultiplatform::class)
@kotlin.jvm.ImplicitlyActualizedByJvmDeclaration
expect class ImplicitlyActualizedByJvmDeclarationOnExpectOnly

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
package kotlin

@OptIn(ExperimentalMultiplatform::class)
actual annotation class <!ACTUAL_WITHOUT_EXPECT!>OptionalExpectationOnExpectOnly<!>

actual fun <!ACTUAL_WITHOUT_EXPECT!>skippedAnnotationsOnExpectOnly<!>() {}

actual class <!ACTUAL_WITHOUT_EXPECT!>ImplicitlyActualizedByJvmDeclarationOnExpectOnly<!>
