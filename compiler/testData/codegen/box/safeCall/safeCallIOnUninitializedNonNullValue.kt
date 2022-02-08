abstract class Base() {
    init {
        foo()
    }

    abstract fun foo()
}

class Derived(val x: String) : Base() {
    override fun foo() {
        x?.length
    }
}

fun box(): String {
    Derived("")
    return "OK"
}
