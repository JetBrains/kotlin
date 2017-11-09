interface IFooBar {
    fun foo()
    val bar: Int
}

class Host {
    fun IFooBar.<!EXTENSION_SHADOWED_BY_MEMBER!>foo<!>() {}
    val IFooBar.<!EXTENSION_SHADOWED_BY_MEMBER!>bar<!>: Int get() = 42
}