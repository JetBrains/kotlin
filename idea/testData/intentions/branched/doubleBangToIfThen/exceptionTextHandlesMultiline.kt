// WITH_RUNTIME
fun main(args: Array<String>) {
    val t = doSomething("one" + 1,
            "two",
            3 * 4)<caret>!!
}

fun doSomething(vararg a: Any): Any? = null
