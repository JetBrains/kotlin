interface I {
    fun doA(): A
}

interface U {
    fun doA(): A
}

open class C {
    fun doB(b: B = B()): Int = 42
}
