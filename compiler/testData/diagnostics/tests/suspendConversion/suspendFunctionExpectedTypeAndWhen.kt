// FIR_IDENTICAL
// ISSUE: KT-57446

fun test1() : suspend (Int) -> Unit = when {
    true -> { _ -> }
    else -> { _ -> }
}

fun test2() : suspend (Int) -> Unit = when {
    true -> { x -> foo(x) }
    else -> { y -> foo(y) }
}

suspend fun foo(x: Int) {}
