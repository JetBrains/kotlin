impl fun foo(n: Int, action: Int.() -> Int) = n.action()

fun test() {
    foo(1) { this + 1 }
}