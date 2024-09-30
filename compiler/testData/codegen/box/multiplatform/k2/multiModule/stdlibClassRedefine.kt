// TARGET_BACKEND: JVM
// WITH_STDLIB
// LANGUAGE: +MultiPlatformProjects
// JVM_ABI_K1_K2_DIFF: KT-67645
// PREFER_IN_TEST_OVER_STDLIB

// MODULE: common
// FILE: array.kt

package kotlin

@kotlin.jvm.JvmInline
value class UIntArray(val delegate: IntArray) : Collection<UInt> {
    override val size: Int
        get() = delegate.size

    override fun isEmpty(): Boolean = null!!
    override fun iterator(): Iterator<UInt> = null!!
    override fun containsAll(elements: Collection<UInt>): Boolean = null!!
    override fun contains(element: UInt): Boolean = null!!
    operator fun get(index: Int): Int = 42
    operator fun set(index: Int, value: UInt) {}
}

// MODULE: main()()(common)
// FILE: test.kt

fun box(): String = if (UIntArray(intArrayOf(1))[0] == 42) "OK" else "Fail"