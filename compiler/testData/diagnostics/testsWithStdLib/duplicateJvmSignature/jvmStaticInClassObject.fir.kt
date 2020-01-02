open class Base {
    fun foo() {}
}

class Derived : Base() {
    companion object {
        @JvmStatic fun foo() {}
    }
}
