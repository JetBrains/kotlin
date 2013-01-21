class MyClass(): Base() {
    fun test2() {
        super.test1()
    }
}

open class Base() {
    fun test1() {}
}