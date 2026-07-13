// TARGET_BACKEND: JVM
// WITH_STDLIB
// PREFER_IN_TEST_OVER_STDLIB
// ALLOW_KOTLIN_PACKAGE

// FILE: declarations.kt
package kotlin

@kotlin.jvm.JvmInline
value class UByteArray(val delegate: ByteArray) : Collection<UByte> {
    override val size: Int
        get() = delegate.size

    override fun isEmpty(): Boolean = null!!
    override fun iterator(): Iterator<UByte> = object : Iterator<UByte> {
        private var index = 0
        override fun hasNext(): Boolean = index < delegate.size
        override fun next(): UByte = delegate[index++].toUByte()
    }
    override fun containsAll(elements: Collection<UByte>): Boolean = null!!
    override fun contains(element: UByte): Boolean = null!!
    operator fun get(index: Int): UByte = delegate[index].toUByte()
    operator fun set(index: Int, value: UByte) {}
}

@kotlin.jvm.JvmInline
value class UShortArray(val delegate: ShortArray) : Collection<UShort> {
    override val size: Int
        get() = delegate.size

    override fun isEmpty(): Boolean = null!!
    override fun iterator(): Iterator<UShort> = object : Iterator<UShort> {
        private var index = 0
        override fun hasNext(): Boolean = index < delegate.size
        override fun next(): UShort = delegate[index++].toUShort()
    }
    override fun containsAll(elements: Collection<UShort>): Boolean = null!!
    override fun contains(element: UShort): Boolean = null!!
    operator fun get(index: Int): UShort = delegate[index].toUShort()
    operator fun set(index: Int, value: UShort) {}
}

@kotlin.jvm.JvmInline
value class UIntArray(val delegate: IntArray) : Collection<UInt> {
    override val size: Int
        get() = delegate.size

    override fun isEmpty(): Boolean = null!!
    override fun iterator(): Iterator<UInt> = object : Iterator<UInt> {
        private var index = 0
        override fun hasNext(): Boolean = index < delegate.size
        override fun next(): UInt = delegate[index++].toUInt()
    }
    override fun containsAll(elements: Collection<UInt>): Boolean = null!!
    override fun contains(element: UInt): Boolean = null!!
    operator fun get(index: Int): UInt = delegate[index].toUInt()
    operator fun set(index: Int, value: UInt) {}
}

@kotlin.jvm.JvmInline
value class ULongArray(val delegate: LongArray) : Collection<ULong> {
    override val size: Int
        get() = delegate.size

    override fun isEmpty(): Boolean = null!!
    override fun iterator(): Iterator<ULong> = object : Iterator<ULong> {
        private var index = 0
        override fun hasNext(): Boolean = index < delegate.size
        override fun next(): ULong = delegate[index++].toULong()
    }
    override fun containsAll(elements: Collection<ULong>): Boolean = null!!
    override fun contains(element: ULong): Boolean = null!!
    operator fun get(index: Int): ULong = delegate[index].toULong()
    operator fun set(index: Int, value: ULong) {}
}

// FILE: test.kt
package test

import kotlin.*

fun test(a: UByteArray): String {
    for (x in a) {
        if (x == 42.toUByte()) {
            return "OK"
        }
    }
    return "Fail"
}

fun test(a: UShortArray): String {
    for (x in a) {
        if (x == 42.toUShort()) {
            return "OK"
        }
    }
    return "Fail"
}

fun test(a: UIntArray): String {
    for (x in a) {
        if (x == 42u) {
            return "OK"
        }
    }
    return "Fail"
}

fun test(a: ULongArray): String {
    for (x in a) {
        if (x == 42uL) {
            return "OK"
        }
    }
    return "Fail"
}

fun testIndices(a: UIntArray): String {
    for (i in a.indices) {
        if (a[i] == 42u) {
            return "OK"
        }
    }
    return "Fail"
}

fun box(): String {
    if (test(UByteArray(byteArrayOf(42.toByte()))) != "OK") return "Fail 1"
    if (test(UShortArray(shortArrayOf(42.toShort()))) != "OK") return "Fail 2"
    if (test(UIntArray(intArrayOf(42))) != "OK") return "Fail 3"
    if (test(ULongArray(longArrayOf(42.toLong()))) != "OK") return "Fail 4"
    if (testIndices(UIntArray(intArrayOf(42))) != "OK") return "Fail 5"
    return "OK"
}
