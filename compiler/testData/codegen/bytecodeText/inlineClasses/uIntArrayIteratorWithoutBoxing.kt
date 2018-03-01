// !LANGUAGE: +InlineClasses

inline class UInt(private val value: Int)

inline class UIntArray(private val intArray: IntArray) {
    operator fun iterator(): UIntIterator = UIntIterator(intArray.iterator()) // create iterator
}

inline class UIntIterator(private val intIterator: IntIterator) : Iterator<UInt> {
    override fun next(): UInt {
        return UInt(intIterator.next())
    }

    override fun hasNext(): Boolean {
        return intIterator.hasNext()
    }
}

fun uIntArrayOf(vararg u: Int): UIntArray = UIntArray(u)

fun test() {
    val a = uIntArrayOf(1, 2, 3, 4)
    for (element in a) {
        takeUInt(element)
    }
}

fun takeUInt(u: UInt) {}

// 0 INVOKESTATIC UInt\$Erased.box
// 0 INVOKEVIRTUAL UInt.unbox

// 0 INVOKEVIRTUAL UIntIterator.iterator
// 1 INVOKESTATIC kotlin/jvm/internal/ArrayIteratorsKt.iterator

// 0 intValue

// inside wrong bridge
// 1 valueOf