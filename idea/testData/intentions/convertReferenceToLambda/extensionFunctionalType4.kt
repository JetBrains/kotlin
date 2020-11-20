fun func2(a: Int, b: Int) {
}

fun Int.foo(block: Int.(Int) -> Unit) {
}

fun main() {
    42.foo(::func2<caret>)
}
