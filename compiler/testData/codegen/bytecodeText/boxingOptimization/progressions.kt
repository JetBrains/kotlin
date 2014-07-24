
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
    foo((1.0.toDouble()..2.0.toDouble())) { x -> x + 1 }
    foo((1.0f..2.0f)) { x -> x + 1 }
    foo((1.toByte()..100.toByte())) { x -> x }
    foo((1.toShort()..100.toShort())) { x -> x }
    foo(('a'..'z')) { x -> x }

    foo(IntRange(1, 100)) { x -> x + 1 }
    foo(LongRange(1L, 100L)) { x -> x + 1 }
    foo(DoubleRange(1.0, 2.0)) { x -> x + 1 }
    foo(FloatRange(1.0f, 2.0f)) { x -> x + 1 }
    foo(ByteRange(1.toByte(), 100.toByte())) { x -> x }
    foo(ShortRange(1.toShort(), 100.toShort())) { x -> x }
    foo(CharRange('a', 'z')) { x -> x }
}

// 1 next\s\(
// 2 nextInt
// 2 nextLong
// 2 nextDouble
// 2 nextFloat
// 2 nextByte
// 2 nextShort
// 2 nextChar
// 0 Value\s\(\)
