// TARGET_BACKEND: NATIVE

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:OptIn(kotlin.ExperimentalStdlibApi::class)

import kotlin.native.concurrent.*
import kotlin.concurrent.*
import kotlin.native.internal.*
import kotlin.reflect.KMutableProperty0


// Overload resolution is not working in K2 with Supress INVISIBLE_REFERENCE.
// But resolving constants and annotations work
// So we are creating local copies of this intrinsic for test

@TypedIntrinsic(IntrinsicType.GET_AND_ADD_FIELD)
internal external fun KMutableProperty0<Short>.getAndAddFieldLocal(delta: Short): Short
@TypedIntrinsic(IntrinsicType.GET_AND_ADD_FIELD)
internal external fun KMutableProperty0<Int>.getAndAddFieldLocal(newValue: Int): Int
@TypedIntrinsic(IntrinsicType.GET_AND_ADD_FIELD)
internal external fun KMutableProperty0<Long>.getAndAddFieldLocal(newValue: Long): Long
@TypedIntrinsic(IntrinsicType.GET_AND_ADD_FIELD)
internal external fun KMutableProperty0<Byte>.getAndAddFieldLocal(newValue: Byte): Byte


interface Wrapper<T> {
    fun get(): T
    fun set(new: T): Unit
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
    override fun get(): Int = this::x.atomicGetField()
    override fun set(new: Int) = this::x.atomicSetField(new)
    override fun compareAndSwap(expected: Int, new: Int) = this::x.compareAndExchangeField(expected, new)
    override fun compareAndSet(expected: Int, new: Int) = this::x.compareAndSetField(expected, new)
    override fun getAndSet(new: Int) = this::x.getAndSetField(new)
    override fun getAndAdd(delta: Int) = this::x.getAndAddFieldLocal(delta)
}

class LongWrapper(@Volatile var x : Long) : IncWrapper<Long> {
    override fun get(): Long = this::x.atomicGetField()
    override fun set(new: Long) = this::x.atomicSetField(new)
    override fun compareAndSwap(expected: Long, new: Long) = this::x.compareAndExchangeField(expected, new)
    override fun compareAndSet(expected: Long, new: Long) = this::x.compareAndSetField(expected, new)
    override fun getAndSet(new: Long) = this::x.getAndSetField(new)
    override fun getAndAdd(delta: Long) = this::x.getAndAddFieldLocal(delta)
}

class ShortWrapper(@Volatile var x : Short) : IncWrapper<Short> {
    override fun get(): Short = this::x.atomicGetField()
    override fun set(new: Short) = this::x.atomicSetField(new)
    override fun compareAndSwap(expected: Short, new: Short) = this::x.compareAndExchangeField(expected, new)
    override fun compareAndSet(expected: Short, new: Short) = this::x.compareAndSetField(expected, new)
    override fun getAndSet(new: Short) = this::x.getAndSetField(new)
    override fun getAndAdd(delta: Short) = this::x.getAndAddFieldLocal(delta)
}

class ByteWrapper(@Volatile var x : Byte) : IncWrapper<Byte> {
    override fun get(): Byte = this::x.atomicGetField()
    override fun set(new: Byte) = this::x.atomicSetField(new)
    override fun compareAndSwap(expected: Byte, new: Byte) = this::x.compareAndExchangeField(expected, new)
    override fun compareAndSet(expected: Byte, new: Byte) = this::x.compareAndSetField(expected, new)
    override fun getAndSet(new: Byte) = this::x.getAndSetField(new)
    override fun getAndAdd(delta: Byte) = this::x.getAndAddFieldLocal(delta)
}

class BooleanWrapper(@Volatile var x : Boolean) : Wrapper<Boolean> {
    override fun get(): Boolean = this::x.atomicGetField()
    override fun set(new: Boolean) = this::x.atomicSetField(new)
    override fun compareAndSwap(expected: Boolean, new: Boolean) = this::x.compareAndExchangeField(expected, new)
    override fun compareAndSet(expected: Boolean, new: Boolean) = this::x.compareAndSetField(expected, new)
    override fun getAndSet(new: Boolean) = this::x.getAndSetField(new)
}

class StringWrapper(@Volatile var x : String) : RefWrapper<String> {
    override fun get(): String = this::x.atomicGetField()
    override fun set(new: String) = this::x.atomicSetField(new)
    override fun compareAndSwap(expected: String, new: String) = this::x.compareAndExchangeField(expected, new)
    override fun compareAndSet(expected: String, new: String) = this::x.compareAndSetField(expected, new)
    override fun getAndSet(new: String) = this::x.getAndSetField(new)
}

class GenericWrapper<T>(@Volatile var x : T) : RefWrapper<T> {
    override fun get(): T = this::x.atomicGetField()
    override fun set(new: T) = this::x.atomicSetField(new)
    override fun compareAndSwap(expected: T, new: T) = this::x.compareAndExchangeField(expected, new)
    override fun compareAndSet(expected: T, new: T) = this::x.compareAndSetField(expected, new)
    override fun getAndSet(new: T) = this::x.getAndSetField(new)
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
    if (w.get() != one) return "FAIL 12"
    w.set(three)
    if (w.get() != three) return "FAIL 13"
    w.set(one)
    if (w.get() != one) return "FAIL 14"
    if (w is IncWrapper<T>) {
        if (w.getAndAdd(one) != one) return "FAIL 15"
        if (w.get() != two) return "FAIL 16"
        if (w.getAndAdd(one) != two) return "FAIL 17"
        if (w.getAndAdd(one) != three) return "FAIL 18"
    }
    return null
}


fun box() : String {
    test(1, 2, 3, ::IntWrapper)?.let { return "Int: $it" }
    test(1, 2, 3, ::LongWrapper)?.let { return "Long: $it" }
    test(1, 2, 3, ::ShortWrapper)?.let { return "Short: $it" }
    test(1, 2, 3, ::ByteWrapper)?.let { return "Byte: $it" }
    test(true, false, true, ::BooleanWrapper)?.let { return "Boolean: $it" }
    test("1", "2", "3", ::StringWrapper)?.let { return "String: $it" }
    test("1", "2", "3", { GenericWrapper<String>(it) })?.let { return "Generic<String>: $it" }
    test(1, 2, 3, { GenericWrapper<Int>(it) })?.let { return "Generic<Int>: $it" }
    return "OK"
}