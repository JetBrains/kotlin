
fun Int.foo(inc0:Int, inc:Int = 0) = this + inc0 + inc

fun main(arg:Array<String>) {
    val v = 42.foo(0)
    if (v != 42) {
        println("test failed v:$v expected:42")
        throw Error()
    }
}
