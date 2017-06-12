// FLOW: IN

fun foo(<caret>n: Int, s: String = "???") {

}

fun test() {
    foo(1)
    foo(1, "2")
    foo(1, s = "2")
    foo(n = 1, s = "2")
    foo(s = "2", n = 1)
}