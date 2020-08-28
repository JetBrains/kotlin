fun func(a: Int) {
}

fun Int.foo(block: (Int) -> Unit) {
}

fun main() {
    42.foo(::func<caret>)
}