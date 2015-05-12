fun doSomething<T>(a: T) {}

fun test() {
    class Test{
        fun contains(a: Int) : Boolean = true
    }
    val test = Test()
    doSomething(test.c<caret>ontains(0).toString())
}
