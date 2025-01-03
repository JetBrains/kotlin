// RUN_PIPELINE_TILL: FIR2IR
// IGNORE_FIR_DIAGNOSTICS
// MODULE: m1-common
// FILE: common.kt
@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Ann<!>

<!CONFLICTING_OVERLOADS!>expect fun <@Ann A> inMethod()<!>

<!CONFLICTING_OVERLOADS!>expect fun <A, @Ann B> inMethodTwoParams()<!>

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>InClass<!><@Ann A>

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>ViaTypealias<!><@Ann A>

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>TypealiasParamNotAccepted<!><@Ann A>

<!CONFLICTING_OVERLOADS!>expect fun <!NO_ACTUAL_FOR_EXPECT{JVM}!><@Ann A, @Ann B><!> withIncompatibility()<!>

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual fun <A> <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>inMethod<!>() {}

actual fun <@Ann A, B> <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>inMethodTwoParams<!>() {}

actual class <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>InClass<!><A>

class ViaTypealiasImpl<@Ann A>

actual typealias ViaTypealias<A> = ViaTypealiasImpl<A>

class TypealiasParamNotAcceptedImpl<A>

actual typealias <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>TypealiasParamNotAccepted<!><@Ann A> = TypealiasParamNotAcceptedImpl<A>

actual fun <!ACTUAL_WITHOUT_EXPECT!><A><!> withIncompatibility() {}
