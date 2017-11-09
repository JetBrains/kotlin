interface IFoo {
    fun foo()
}

fun outer() {
    fun IFoo.<!EXTENSION_SHADOWED_BY_MEMBER!>foo<!>() {}
}