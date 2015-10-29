
inline fun <R, T : Iterable<R>> foo(x : T, block : (R) -> R) : R {
    val y = x.iterator()
    if (y.hasNext()) {
        return block(y.next())
    } else {
        throw RuntimeException()
    }
}

fun bar() {
    foo((1..100)) { x -> x + 1 }
    foo((1L..100L)) { x -> x + 1 }
    foo((1.toByte()..100.toByte())) { x -> x }
    foo((1.toShort()..100.toShort())) { x -> x }
    foo(('a'..'z')) { x -> x }

    foo(IntRange(1, 100)) { x -> x + 1 }
    foo(LongRange(1L, 100L)) { x -> x + 1 }
    foo(CharRange('a', 'z')) { x -> x }
}

// 1 next\s\(
// 4 nextInt
// 2 nextLong
// 0 nextByte
// 0 nextShort
// 2 nextChar
// 0 Value\s\(\)
