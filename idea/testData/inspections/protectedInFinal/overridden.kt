open class Test {
    open protected fun test() {}
}

class Subclass : Test() {
    protected override fun test() {
        super.test()
    }
}