// MODULE: m1-common
// FILE: common.kt
@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class Ann

expect fun <@Ann A> inMethod()

expect fun <A, @Ann B> inMethodTwoParams()

expect class InClass<@Ann A>

expect class ViaTypealias<@Ann A>

expect class TypealiasParamNotAccepted<@Ann A>

expect fun <@Ann A, @Ann B> withIncompatibility()

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual fun <A> inMethod() {}

actual fun <@Ann A, B> inMethodTwoParams() {}

actual class InClass<A>

class ViaTypealiasImpl<@Ann A>

actual typealias ViaTypealias<A> = ViaTypealiasImpl<A>

class TypealiasParamNotAcceptedImpl<A>

actual typealias TypealiasParamNotAccepted<@Ann A> = TypealiasParamNotAcceptedImpl<A>

actual fun <A> <!ACTUAL_WITHOUT_EXPECT!>withIncompatibility<!>() {}
