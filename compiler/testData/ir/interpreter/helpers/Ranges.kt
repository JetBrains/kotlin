@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("RangesKt")

package kotlin.ranges

public infix fun Int.until(to: Byte): IntRange {
    return this .. (to.toInt() - 1).toInt()
}
public infix fun Long.until(to: Byte): LongRange {
    return this .. (to.toLong() - 1).toLong()
}
public infix fun Byte.until(to: Byte): IntRange {
    return this.toInt() .. (to.toInt() - 1).toInt()
}
public infix fun Short.until(to: Byte): IntRange {
    return this.toInt() .. (to.toInt() - 1).toInt()
}

public infix fun Char.until(to: Char): CharRange {
    if (to <= '\u0000') return CharRange.EMPTY
    return this .. (to - 1).toChar()
}

public infix fun Int.until(to: Int): IntRange {
    if (to <= Int.MIN_VALUE) return IntRange.EMPTY
    return this .. (to - 1).toInt()
}
public infix fun Long.until(to: Int): LongRange {
    return this .. (to.toLong() - 1).toLong()
}
public infix fun Byte.until(to: Int): IntRange {
    if (to <= Int.MIN_VALUE) return IntRange.EMPTY
    return this.toInt() .. (to - 1).toInt()
}
public infix fun Short.until(to: Int): IntRange {
    if (to <= Int.MIN_VALUE) return IntRange.EMPTY
    return this.toInt() .. (to - 1).toInt()
}

public infix fun Int.until(to: Long): LongRange {
    if (to <= Long.MIN_VALUE) return LongRange.EMPTY
    return this.toLong() .. (to - 1).toLong()
}
public infix fun Long.until(to: Long): LongRange {
    if (to <= Long.MIN_VALUE) return LongRange.EMPTY
    return this .. (to - 1).toLong()
}
public infix fun Byte.until(to: Long): LongRange {
    if (to <= Long.MIN_VALUE) return LongRange.EMPTY
    return this.toLong() .. (to - 1).toLong()
}
public infix fun Short.until(to: Long): LongRange {
    if (to <= Long.MIN_VALUE) return LongRange.EMPTY
    return this.toLong() .. (to - 1).toLong()
}

public infix fun Int.until(to: Short): IntRange {
    return this .. (to.toInt() - 1).toInt()
}
public infix fun Long.until(to: Short): LongRange {
    return this .. (to.toLong() - 1).toLong()
}
public infix fun Byte.until(to: Short): IntRange {
    return this.toInt() .. (to.toInt() - 1).toInt()
}
public infix fun Short.until(to: Short): IntRange {
    return this.toInt() .. (to.toInt() - 1).toInt()
}

public infix fun UByte.until(to: UByte): UIntRange {
    if (to <= UByte.MIN_VALUE) return UIntRange.EMPTY
    return this.toUInt() .. (to - 1u).toUInt()
}

public infix fun UInt.until(to: UInt): UIntRange {
    if (to <= UInt.MIN_VALUE) return UIntRange.EMPTY
    return this .. (to - 1u).toUInt()
}

public infix fun ULong.until(to: ULong): ULongRange {
    if (to <= ULong.MIN_VALUE) return ULongRange.EMPTY
    return this .. (to - 1u).toULong()
}

public infix fun UShort.until(to: UShort): UIntRange {
    if (to <= UShort.MIN_VALUE) return UIntRange.EMPTY
    return this.toUInt() .. (to - 1u).toUInt()
}