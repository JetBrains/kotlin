// WITH_RUNTIME
fun main(args: Array<String>) {
    var a: String? = "A"
    doSomething(a<caret>!!)
}

fun doSomething(a: Any){}
