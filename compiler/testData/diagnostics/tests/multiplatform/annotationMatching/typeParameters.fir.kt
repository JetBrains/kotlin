// MODULE: m1-common
// FILE: common.kt
@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class Ann

expect fun <@Ann A> inMethod()

expect fun <A, @Ann B> inMethodTwoParams()

expect class InClass<@Ann A>

expect class ViaTypealias<@Ann A>

expect class TypealiasParamNotAccepted<@Ann A>

<!INCOMPATIBLE_MATCHING{JVM}!>expect fun <@Ann A, @Ann B> withIncompatibility()<!>

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual fun <A> <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>inMethod<!>() {}<!>

<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual fun <@Ann A, B> <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>inMethodTwoParams<!>() {}<!>

<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual class <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>InClass<!><A><!>

class ViaTypealiasImpl<@Ann A>

actual typealias ViaTypealias<A> = ViaTypealiasImpl<A>

class TypealiasParamNotAcceptedImpl<A>

<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual typealias <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>TypealiasParamNotAccepted<!><@Ann A> = TypealiasParamNotAcceptedImpl<A><!>

actual fun <A> <!ACTUAL_WITHOUT_EXPECT!>withIncompatibility<!>() {}
