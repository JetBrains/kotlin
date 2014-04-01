fun doSomething<T>(a: T) {}

fun test() {
    class Test{
        fun contains(fn: () -> Boolean) : Boolean = true
    }
    val test = Test()
    doSomething(test.c<caret>ontains { true }.toString())
}
