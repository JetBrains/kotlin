// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: BACKEND
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
annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Ann<!>

@Ann
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>OnClass<!>

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>OnMember<!> {
    @Ann
    fun onMember()
}

@Ann
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>ViaTypealias<!>

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>MemberScopeViaTypealias<!> {
    @Ann
    fun foo()
}

annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>WithArg<!>(val s: String)

<!CONFLICTING_OVERLOADS!>@WithArg("str")
expect fun withDifferentArg()<!>

<!CONFLICTING_OVERLOADS!>expect fun inValueParam(@Ann arg: String)<!>

<!CONFLICTING_OVERLOADS!>expect fun <@Ann T> inTypeParam()<!>

@get:Ann
expect val <!REDECLARATION!>onGetter<!>: String

<!CONFLICTING_OVERLOADS!>expect fun onType(param: @Ann Any)<!>

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

actual fun onType(param: Any) {}
