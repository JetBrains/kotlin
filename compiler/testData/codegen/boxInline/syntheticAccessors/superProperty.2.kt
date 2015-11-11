package test

open class A {
    open val test = "OK"
}

object X : A() {
    override val test: String
        get() = "fail"

    inline fun doTest(): String {
        return super.test
    }
}
