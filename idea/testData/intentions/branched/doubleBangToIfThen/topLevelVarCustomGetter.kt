// WITH_RUNTIME
var a: String?
    get() = ""
    set(v) {}

fun main(args: Array<String>) {
    println(a<caret>!!)
}
