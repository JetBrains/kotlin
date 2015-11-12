// "Move to constructor parameters" "true"
annotation class foo

open class A(s: String) {
    <caret>private @foo     val /*1*/ n: /* 2 */ Int
}

class B : A("")

fun test() {
    val a = A("")
}