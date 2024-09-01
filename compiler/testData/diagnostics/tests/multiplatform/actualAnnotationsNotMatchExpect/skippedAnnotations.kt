// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt
package kotlin

<!OPT_IN_WITHOUT_ARGUMENTS!>@OptIn(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!><!UNRESOLVED_REFERENCE!>ExperimentalMultiplatform<!>::class<!>)<!>
@<!UNRESOLVED_REFERENCE!>OptionalExpectation<!>
expect annotation class OptionalExpectationOnExpectOnly

@RequiresOptIn
annotation class MyOptIn

@SinceKotlin("1.8")
@Deprecated(message = "Some text")
@DeprecatedSinceKotlin("1.8")
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@MyOptIn
@WasExperimental(MyOptIn::class)
@kotlin.internal.<!UNRESOLVED_REFERENCE!>RequireKotlin<!>(<!DEBUG_INFO_MISSING_UNRESOLVED!>version<!> = "1.8")
@OptIn(MyOptIn::class)
expect fun skippedAnnotationsOnExpectOnly()

<!OPT_IN_WITHOUT_ARGUMENTS!>@OptIn(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!><!UNRESOLVED_REFERENCE!>ExperimentalMultiplatform<!>::class<!>)<!>
@kotlin.<!UNRESOLVED_REFERENCE!>jvm<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>ImplicitlyActualizedByJvmDeclaration<!>
expect class ImplicitlyActualizedByJvmDeclarationOnExpectOnly

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
package kotlin

@OptIn(ExperimentalMultiplatform::class)
actual annotation class OptionalExpectationOnExpectOnly

actual fun skippedAnnotationsOnExpectOnly() {}

actual class ImplicitlyActualizedByJvmDeclarationOnExpectOnly
