// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER, -DEPRECATION
@file:Suppress("OPT_IN_USAGE")

external interface I

external class C {
    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_FUN, WRONG_EXTERNAL_DECLARATION!>@nativeInvoke
    fun I.memberExtension(a: Int): Int<!>
}

<!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_FUN!>@nativeInvoke
fun I.withBody(a: Int): Int<!> = 1

<!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_FUN!>@nativeInvoke
operator fun C.invoke(a: Int): Int<!> = 1

<!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_FUN!>@nativeInvoke
fun I.withDefinedExternally(a: Int): Int<!> = <!CALL_TO_DEFINED_EXTERNALLY_FROM_NON_EXTERNAL_DECLARATION!>definedExternally<!>

<!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_FUN, WRONG_EXTERNAL_DECLARATION!>@nativeInvoke
external fun I.externalExtension(a: Int): Int<!>
