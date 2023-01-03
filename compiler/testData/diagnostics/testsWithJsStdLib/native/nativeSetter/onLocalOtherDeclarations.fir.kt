// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -NON_TOPLEVEL_CLASS_DECLARATION, -DEPRECATION

fun foo() {
    @nativeSetter
    fun toplevelFun(): Any = 0

    <!WRONG_ANNOTATION_TARGET!>@nativeSetter<!>
    val toplevelVal = 0

    <!WRONG_ANNOTATION_TARGET!>@nativeSetter<!>
    class Foo {}
}
