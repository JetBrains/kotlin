// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER, -DEPRECATION

external interface I

external class C {
    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN, WRONG_EXTERNAL_DECLARATION!>@nativeInvoke
    fun I.memberExtension(a: Int): Int<!>
}

@nativeInvoke
fun I.withBody(a: Int): Int = 1

@nativeInvoke
operator fun C.invoke(a: Int): Int = 1

@nativeInvoke
fun I.withDefinedExternally(a: Int): Int = definedExternally

<!WRONG_EXTERNAL_DECLARATION!>@nativeInvoke
external fun I.externalExtension(a: Int): Int<!>
