// ISSUE: KT-57477

fun f(block: (Int) -> Unit) {}
fun f(block: (Int, Int) -> Unit) {}

fun g(block: Int.() -> Unit) {}
fun g(block: (Int, Int) -> Unit) {}

fun test() {
    f {}
    g {}
}
