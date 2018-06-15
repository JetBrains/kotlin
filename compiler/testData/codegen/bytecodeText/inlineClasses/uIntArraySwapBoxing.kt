// !LANGUAGE: +InlineClasses

inline class UInt(private val value: Int) {
    fun asInt() = value
}

inline class UIntArray(private val intArray: IntArray) {
    operator fun get(index: Int): UInt = UInt(intArray[index])

    operator fun set(index: Int, value: UInt) {
        intArray[index] = value.asInt()
    }
}

fun UIntArray.swap(i: Int, j: Int) {
    this[j] = this[i].also { this[i] = this[j] }
}

// 0 INVOKEVIRTUAL UInt.unbox
// 0 INVOKESTATIC UInt\$Erased.box

// 0 intValue
// 0 valueOf