open class Base {
    fun doSomething() {

    }
}

class X(val action: () -> Unit) { }

class Foo : Base() {
    inner class Bar() {
        val x = X({ doSomething() })
    }
}

fun box() : String {
    Foo().Bar()
    return "OK"
}
