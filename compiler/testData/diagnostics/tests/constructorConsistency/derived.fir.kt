open class Base(val x: String) {
    fun foo() = bar()

    open fun bar() = -1
}

class Derived(x: String): Base(x) {
    // It's still dangerous: we're not sure that foo() does not call some open function inside
    val y = foo()
    val z = x
}
