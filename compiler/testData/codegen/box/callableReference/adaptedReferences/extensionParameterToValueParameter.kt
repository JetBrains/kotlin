fun foo(x: (a: Int, y: String) -> Unit) { }
fun bar(x: Int.(y: String) -> Unit) {
    4.x("")
    x(4, "")
}

fun test1(a: Int, y: String){}
fun Int.test2(y: String) {}

fun box(): String {
    foo(::test1)
    foo(Int::test2)
    bar(::test1)
    bar(Int::test2)
    return "OK"
}