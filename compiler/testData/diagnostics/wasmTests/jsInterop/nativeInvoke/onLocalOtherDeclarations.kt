// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -NON_TOPLEVEL_CLASS_DECLARATION, -DEPRECATION
@file:Suppress("OPT_IN_USAGE")

fun foo() {
    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_FUN!>@nativeInvoke
    fun toplevelFun()<!> {}

    <!WRONG_ANNOTATION_TARGET!>@nativeInvoke<!>
    val toplevelVal = 0

    <!WRONG_ANNOTATION_TARGET!>@nativeInvoke<!>
    class Foo {}
}
