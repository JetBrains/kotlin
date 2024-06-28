// RENDER_DIAGNOSTICS_FULL_TEXT
// RENDER_IR_DIAGNOSTICS_FULL_TEXT
// MODULE: m1-common
// FILE: common.kt
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.CLASS,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.TYPE_PARAMETER,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.TYPE,
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

expect fun onType(param: @Ann Any)

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual class OnClass<!>

actual class OnMember {
    <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual fun onMember() {}<!>
}

class ViaTypealiasImpl

<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual typealias ViaTypealias = ViaTypealiasImpl<!>

class MemberScopeViaTypealiasImpl {
    fun foo() {}
}
<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual typealias MemberScopeViaTypealias = MemberScopeViaTypealiasImpl<!>

<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>@WithArg("other str")
actual fun withDifferentArg() {}<!>

<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual fun inValueParam(arg: String) {}<!>

<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual fun <T> inTypeParam() {}<!>

<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual val onGetter: String = ""<!>

<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual fun onType(param: Any) {}<!>
