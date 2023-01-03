// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -NON_TOPLEVEL_CLASS_DECLARATION, -DEPRECATION

fun foo() {
    @nativeInvoke
    fun toplevelFun() {}

    <!WRONG_ANNOTATION_TARGET!>@nativeInvoke<!>
    val toplevelVal = 0

    <!WRONG_ANNOTATION_TARGET!>@nativeInvoke<!>
    class Foo {}
}
