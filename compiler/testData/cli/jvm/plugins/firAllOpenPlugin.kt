package foo

annotation class AllOpen

@AllOpen
class Base {
    fun method() {}
    val property = "hello"
}

class Derived : Base() {
    override fun method() {}
    override val property = "world"
}
