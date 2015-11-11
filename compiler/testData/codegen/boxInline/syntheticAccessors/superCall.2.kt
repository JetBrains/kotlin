package test

open class A {
    open fun test() = "OK"
}

object X : A() {
    override fun test(): String {
        return "fail"
    }

    inline fun doTest(): String {
        return super.test()
    }
}
