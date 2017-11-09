class My {
    fun foo(): String? = null
}

fun test(my: My?) {
    if (<caret>my != null && my.foo() != null) {}
}