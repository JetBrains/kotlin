// RENDER_DIAGNOSTICS_FULL_TEXT
// MODULE: m1-common
// FILE: common.kt
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.CLASS,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.TYPE_PARAMETER,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
)
annotation class Ann

@Ann
expect class OnClass

expect class OnMember {
    @Ann
    fun onMember()
}

@Ann
expect class ViaTypealias

expect class MemberScopeViaTypealias {
    @Ann
    fun foo()
}

annotation class WithArg(val s: String)

@WithArg("str")
expect fun withDifferentArg()

expect fun inValueParam(@Ann arg: String)

expect fun <@Ann T> inTypeParam()

@get:Ann
expect val onGetter: String

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual class <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>OnClass<!>

actual class OnMember {
    actual fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>onMember<!>() {}
}

class ViaTypealiasImpl

actual typealias <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>ViaTypealias<!> = ViaTypealiasImpl

class MemberScopeViaTypealiasImpl {
    fun foo() {}
}
actual typealias <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>MemberScopeViaTypealias<!> = MemberScopeViaTypealiasImpl

@WithArg("other str")
actual fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>withDifferentArg<!>() {}

actual fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>inValueParam<!>(arg: String) {}

actual fun <T> <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>inTypeParam<!>() {}

actual val <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>onGetter<!>: String = ""
