// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -NON_TOPLEVEL_CLASS_DECLARATION, -DEPRECATION

fun foo() {
    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>@nativeInvoke
    fun toplevelFun()<!> {}

    <!WRONG_ANNOTATION_TARGET!>@nativeInvoke<!>
    val toplevelVal = 0

    <!WRONG_ANNOTATION_TARGET!>@nativeInvoke<!>
    class Foo {}
}
