// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +CollectionLiterals
// WITH_STDLIB
// ISSUE: KT-81722

@file:OptIn(ExperimentalCollectionLiterals::class)

fun <T> T.run(block: T.() -> Unit) {
    block()
}

fun box(): String {
    List.run {
        toString()
        of(1, 2, 3)
    }
    MutableList.run {
        toString()
        of(1, 2, 3)
    }
    Set.run {
        toString()
        of(1, 2, 3)
    }
    MutableSet.run {
        toString()
        of(1, 2, 3)
    }
    Sequence.run {
        toString()
        of(1, 2, 3)
    }
    IntArray.run {
        toString()
        of(*[1, 2, 3])
    }
    LongArray.run {
        toString()
        of(1, 2, 3)
    }
    ShortArray.run {
        toString()
        of(*[1, 2, 3])
    }
    ByteArray.run {
        toString()
        of(1, 2, 3)
    }
    CharArray.run {
        toString()
        of(*['a', 'b', 'c'])
    }
    BooleanArray.run {
        toString()
        of(true, false, true)
    }
    FloatArray.run {
        toString()
        of(*[1.0f, 2.0f, 3.0f])
    }
    DoubleArray.run {
        toString()
        of(1.0, 2.0, 3.0)
    }
    UIntArray.run {
        toString()
        of(*[1u, 2u, 3u])
    }
    ULongArray.run {
        toString()
        of(1uL, 2uL, 3uL)
    }
    UShortArray.run {
        toString()
        of(*[1.toUShort(), 2.toUShort(), 3.toUShort()])
    }
    UByteArray.run {
        toString()
        of(1.toUByte(), 2.toUByte(), 3.toUByte())
    }
    Array.run {
        toString()
        of<Any?>(1, null, "last")
    }
    return "OK"
}
