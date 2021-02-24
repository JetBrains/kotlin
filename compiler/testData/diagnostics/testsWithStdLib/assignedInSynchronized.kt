// FIR_IDENTICAL
class A {
    fun test() {
        val a: A
        synchronized(this) {
            if (bar()) throw RuntimeException()
            a = A()
        }
        a.bar()
    }

    fun bar() = false
}
