fun <T> doSomething(a: T) {}

fun test() {
    class Test{
        operator fun contains(fn: () -> Boolean) : Boolean = true
    }
    val test = Test()
    doSomething(test.c<caret>ontains { true }.toString())
}
