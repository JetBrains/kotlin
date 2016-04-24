fun f(a: (Int) -> Unit) = a as Int.() -> Unit

fun f1(a: Int.() -> Unit) = a as (Int) -> Unit