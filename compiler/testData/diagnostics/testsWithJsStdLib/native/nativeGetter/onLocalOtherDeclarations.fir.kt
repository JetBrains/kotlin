// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -NON_TOPLEVEL_CLASS_DECLARATION, -DEPRECATION

fun foo() {
    @nativeGetter
    fun toplevelFun(): Any = 0

    <!WRONG_ANNOTATION_TARGET!>@nativeGetter<!>
    val toplevelVal = 0

    <!WRONG_ANNOTATION_TARGET!>@nativeGetter<!>
    class Foo {}
}
