// TARGET_BACKEND: JVM_IR
// JVM_ABI_K1_K2_DIFF: KT-67645
// LANGUAGE: +MultiPlatformProjects
// STDLIB_COMPILATION

// MODULE: common
// FILE: annotation.kt

package kotlin.jvm

annotation class JvmInline

// FILE: common.kt
package kotlin

@kotlin.jvm.JvmInline
value class UByte(val data: Byte) {
    public fun toByte(): Byte = null!!
    public fun toShort(): Short = null!!
    public fun toInt(): Int = null!!
    public fun toLong(): Long = null!!
    public fun toUByte(): Int = null!!
    public fun toUShort(): Int = null!!
    public fun toUInt(): UInt = null!!
    public fun toULong(): Int = null!!
}

@kotlin.jvm.JvmInline
value class UShort(val data: Short) {
    public fun toByte(): Byte = null!!
    public fun toShort(): Short = null!!
    public fun toInt(): Int = null!!
    public fun toLong(): Long = null!!
    public fun toUByte(): Int = null!!
    public fun toUShort(): Int = null!!
    public fun toUInt(): UInt = null!!
    public fun toULong(): Int = null!!
}

@kotlin.jvm.JvmInline
value class UInt(val data: Int) {
    public fun toByte(): Byte = null!!
    public fun toShort(): Short = null!!
    public fun toInt(): Int = null!!
    public fun toLong(): Long = null!!
    public fun toUByte(): Int = null!!
    public fun toUShort(): Int = null!!
    public fun toUInt(): UInt = null!!
    public fun toULong(): Int = null!!
}

@kotlin.jvm.JvmInline
value class ULong(val data: Long) {
    public fun toByte(): Byte = null!!
    public fun toShort(): Short = null!!
    public fun toInt(): Int = null!!
    public fun toLong(): Long = null!!
    public fun toUByte(): Int = null!!
    public fun toUShort(): Int = null!!
    public fun toUInt(): UInt = null!!
    public fun toULong(): Int = null!!
}

@kotlin.jvm.JvmInline
value class UByteArray(val delegate: ByteArray) : Collection<UByte> {
    override val size: Int
        get() = delegate.size

    override fun isEmpty(): Boolean = null!!
    override fun iterator(): Iterator<UByte> = null!!
    override fun containsAll(elements: Collection<UByte>): Boolean = null!!
    override fun contains(element: UByte): Boolean = null!!
    operator fun get(index: Int): UByte = UByte(42.toByte())
    operator fun set(index: Int, value: UByte) {}
}

@kotlin.jvm.JvmInline
value class UShortArray(val delegate: ShortArray) : Collection<UShort> {
    override val size: Int
        get() = delegate.size

    override fun isEmpty(): Boolean = null!!
    override fun iterator(): Iterator<UShort> = null!!
    override fun containsAll(elements: Collection<UShort>): Boolean = null!!
    override fun contains(element: UShort): Boolean = null!!
    operator fun get(index: Int): UShort = UShort(42.toShort())
    operator fun set(index: Int, value: UShort) {}
}

@kotlin.jvm.JvmInline
value class UIntArray(val delegate: IntArray) : Collection<UInt> {
    override val size: Int
        get() = delegate.size

    override fun isEmpty(): Boolean = null!!
    override fun iterator(): Iterator<UInt> = null!!
    override fun containsAll(elements: Collection<UInt>): Boolean = null!!
    override fun contains(element: UInt): Boolean = null!!
    operator fun get(index: Int): UInt = UInt(42)
    operator fun set(index: Int, value: UInt) {}
}

@kotlin.jvm.JvmInline
value class ULongArray(val delegate: LongArray) : Collection<ULong> {
    override val size: Int
        get() = delegate.size

    override fun isEmpty(): Boolean = null!!
    override fun iterator(): Iterator<ULong> = null!!
    override fun containsAll(elements: Collection<ULong>): Boolean = null!!
    override fun contains(element: ULong): Boolean = null!!
    operator fun get(index: Int): ULong = ULong(42.toLong())
    operator fun set(index: Int, value: ULong) {}
}

// MODULE: main()()(common)
// FILE: test.kt

fun test(a: UByteArray): String {
    for (x in a) {
        if (x == UByte(42.toByte())) {
            return "OK"
        }
    }
    return "Fail"
}

fun test(a: UShortArray): String {
    for (x in a) {
        if (x == UShort(42.toShort())) {
            return "OK"
        }
    }
    return "Fail"
}

fun test(a: UIntArray): String {
    for (x in a) {
        if (x == UInt(42)) {
            return "OK"
        }
    }
    return "Fail"
}

fun test(a: ULongArray): String {
    for (x in a) {
        if (x == ULong(42.toLong())) {
            return "OK"
        }
    }
    return "Fail"
}

fun box(): String {
    if (test(UByteArray(byteArrayOf(1.toByte()))) != "OK") return "Fail 1"
    if (test(UShortArray(shortArrayOf(1.toShort()))) != "OK") return "Fail 2"
    if (test(UIntArray(intArrayOf(1))) != "OK") return "Fail 3"
    if (test(ULongArray(longArrayOf(1.toLong()))) != "OK") return "Fail 4"
    return "OK"
}