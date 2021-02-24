// PROBLEM: none
object A {
    fun <T> foo(f: (T) -> Int) {}
}

fun test() {
    A.foo { <caret>_: String -> 24 }
}