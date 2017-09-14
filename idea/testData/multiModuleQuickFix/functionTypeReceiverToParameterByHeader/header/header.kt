// "Convert 'Int.() -> Int' to '(Int) -> Int'" "true"

header fun foo(n: Int, action: <caret>Int.() -> Int): Int

fun test() {
    foo(1) { this + 1 }
}