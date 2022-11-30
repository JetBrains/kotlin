// TARGET_BACKEND: NATIVE
// KT-55904
// IGNORE_BACKEND_K2: NATIVE

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:OptIn(kotlin.ExperimentalStdlibApi::class)

import kotlin.native.concurrent.*
import kotlin.concurrent.*

interface Wrapper<T> {
    fun compareAndSwap(expected: T, new: T): T
    fun compareAndSet(expected: T, new: T): Boolean
    fun getAndSet(expected: T): T
}

interface IncWrapper<T> : Wrapper<T> {
    fun getAndAdd(expected: T): T
}

interface RefWrapper<T> : Wrapper<T> {
}


class IntWrapper(@Volatile var x : Int) : IncWrapper<Int> {
    override fun compareAndSwap(expected: Int, new: Int) = compareAndSwapField(IntWrapper::x, expected, new)
    override fun compareAndSet(expected: Int, new: Int) = compareAndSetField(IntWrapper::x, expected, new)
    override fun getAndSet(new: Int) = getAndSetField(IntWrapper::x, new)
    override fun getAndAdd(delta: Int) = getAndAddField(IntWrapper::x, delta)
}

class LongWrapper(@Volatile var x : Long) : IncWrapper<Long> {
    override fun compareAndSwap(expected: Long, new: Long) = compareAndSwapField(LongWrapper::x, expected, new)
    override fun compareAndSet(expected: Long, new: Long) = compareAndSetField(LongWrapper::x, expected, new)
    override fun getAndSet(new: Long) = getAndSetField(LongWrapper::x, new)
    override fun getAndAdd(delta: Long) = getAndAddField(LongWrapper::x, delta)
}

class ShortWrapper(@Volatile var x : Short) : IncWrapper<Short> {
    override fun compareAndSwap(expected: Short, new: Short) = compareAndSwapField(ShortWrapper::x, expected, new)
    override fun compareAndSet(expected: Short, new: Short) = compareAndSetField(ShortWrapper::x, expected, new)
    override fun getAndSet(new: Short) = getAndSetField(ShortWrapper::x, new)
    override fun getAndAdd(delta: Short) = getAndAddField(ShortWrapper::x, delta)
}

class ByteWrapper(@Volatile var x : Byte) : IncWrapper<Byte> {
    override fun compareAndSwap(expected: Byte, new: Byte) = compareAndSwapField(ByteWrapper::x, expected, new)
    override fun compareAndSet(expected: Byte, new: Byte) = compareAndSetField(ByteWrapper::x, expected, new)
    override fun getAndSet(new: Byte) = getAndSetField(ByteWrapper::x, new)
    override fun getAndAdd(delta: Byte) = getAndAddField(ByteWrapper::x, delta)
}


class StringWrapper(@Volatile var x : String) : RefWrapper<String> {
    override fun compareAndSwap(expected: String, new: String) = compareAndSwapField(StringWrapper::x, expected, new)
    override fun compareAndSet(expected: String, new: String) = compareAndSetField(StringWrapper::x, expected, new)
    override fun getAndSet(new: String) = getAndSetField(StringWrapper::x, new)
}

class GenericWrapper<T>(@Volatile var x : T) : RefWrapper<T> {
    override fun compareAndSwap(expected: T, new: T) = compareAndSwapField(GenericWrapper<T>::x, expected, new)
    override fun compareAndSet(expected: T, new: T) = compareAndSetField(GenericWrapper<T>::x, expected, new)
    override fun getAndSet(new: T) = getAndSetField(GenericWrapper<T>::x, new)
}


inline fun testFail(block: () -> Unit, onSuccess: () -> Nothing) {
    try {
        block()
        onSuccess()
    } catch (ignored: IllegalArgumentException) {
    }
}

fun <T> test(one: T, two: T, three: T, wrap: (T) -> Wrapper<T>) : String? {
    val w = wrap(one)
    if (!isExperimentalMM() && w is RefWrapper<*>) {
        testFail({ w.compareAndSwap(one, two) }) { return "FAIL 1" }
        testFail({ w.compareAndSet(one, two) }) { return "FAIL 2" }
        testFail({ w.getAndSet(one) }) { return "FAIL 3" }
        return null
    }
    if (w.compareAndSet(one, two) != true) return "FAIL 4"
    if (w.compareAndSet(one, two) != false) return "FAIL 5"
    if (w.getAndSet(one) != two) return "FAIL 6"
    if (w.getAndSet(one) != one) return "FAIL 7"
    if (w.compareAndSwap(one, two) != one) return "FAIL 8"
    if (w.compareAndSwap(one, two) != two) return "FAIL 9"
    if (w.compareAndSwap(one, two) != two) return "FAIL 10"
    if (w.compareAndSwap(two, one) != two) return "FAIL 11"
    if (w is IncWrapper<T>) {
        if (w.getAndAdd(one) != one) return "FAIL 12"
        if (w.getAndAdd(one) != two) return "FAIL 13"
        if (w.getAndAdd(one) != three) return "FAIL 14"
    }
    return null
}


fun box() : String {
    test(1, 2, 3, ::IntWrapper)?.let { return "Int: $it" }
    test(1, 2, 3, ::LongWrapper)?.let { return "Long: $it" }
    test(1, 2, 3, ::ShortWrapper)?.let { return "Short: $it" }
    test(1, 2, 3, ::ByteWrapper)?.let { return "Byte: $it" }
    test("1", "2", "3", ::StringWrapper)?.let { return "String: $it" }
    test("1", "2", "3", { GenericWrapper<String>(it) })?.let { return "Generic<String>: $it" }
    test(1, 2, 3, { GenericWrapper<Int>(it) })?.let { return "Generic<Int>: $it" }
    return "OK"
}