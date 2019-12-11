fun f(a: (Int) -> Unit) {
    a as Int.() -> Unit

    f1(a as Int.() -> Unit)
}

fun f1(a: Int.() -> Unit) {
    a as (Int) -> Unit
    f(a as (Int) -> Unit)
}