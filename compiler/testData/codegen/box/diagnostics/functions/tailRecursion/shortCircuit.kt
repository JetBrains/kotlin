// DIAGNOSTICS: -NON_TAIL_RECURSIVE_CALL -NO_TAIL_CALLS_FOUND

tailrec fun foo1(x: Int): Int {
    return maybe(x) ?: foo1(x - 1)
}

fun maybe(x: Int) = x.takeIf { x == 1 }

tailrec fun foo2(x: Int): Boolean {
    return condition(x) || foo2(x - 1)
}

fun condition(x: Int): Boolean = x == 0

fun box() : String = if (foo1(1000000) == 1 && foo2(1000000)) "OK" else "FAIL"