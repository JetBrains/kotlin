// "Convert 'Int.() -> Int' to '(Int) -> Int'" "true"

impl fun foo(n: Int, action: <caret>Int.() -> Int) = n.action()

fun test() {
    foo(1) { this + 1 }
}