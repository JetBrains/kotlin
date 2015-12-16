// WITH_RUNTIME
fun main(args: Array<String>) {
    doSomething("one")<caret>!!
}

fun doSomething(a: Any): Any? = null
