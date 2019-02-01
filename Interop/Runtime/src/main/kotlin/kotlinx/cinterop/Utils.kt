/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.cinterop

interface NativePlacement {

    fun alloc(size: Long, align: Int): NativePointed

    fun alloc(size: Int, align: Int) = alloc(size.toLong(), align)
}

interface NativeFreeablePlacement : NativePlacement {
    fun free(mem: NativePtr)
}

fun NativeFreeablePlacement.free(pointer: CPointer<*>) = this.free(pointer.rawValue)
fun NativeFreeablePlacement.free(pointed: NativePointed) = this.free(pointed.rawPtr)

object nativeHeap : NativeFreeablePlacement {
    override fun alloc(size: Long, align: Int) = nativeMemUtils.alloc(size, align)

    override fun free(mem: NativePtr) = nativeMemUtils.free(mem)
}

private typealias Deferred = () -> Unit

open class DeferScope {

    @PublishedApi
    internal var topDeferred: Deferred? = null

    internal fun executeAllDeferred() {
        topDeferred?.let {
            it.invoke()
            topDeferred = null
        }
    }

    inline fun defer(crossinline block: () -> Unit) {
        val currentTop = topDeferred
        topDeferred = {
            try {
                block()
            } finally {
                // TODO: it is possible to implement chaining without recursion,
                // but it would require using an anonymous object here
                // which is not yet supported in Kotlin Native inliner.
                currentTop?.invoke()
            }
        }
    }
}

abstract class AutofreeScope : DeferScope(), NativePlacement {
    abstract override fun alloc(size: Long, align: Int): NativePointed
}

open class ArenaBase(private val parent: NativeFreeablePlacement = nativeHeap) : AutofreeScope() {

    private var lastChunk: NativePointed? = null

    final override fun alloc(size: Long, align: Int): NativePointed {
        // Reserve space for a pointer:
        val gapForPointer = maxOf(pointerSize, align)

        val chunk = parent.alloc(size = gapForPointer + size, align = gapForPointer)
        nativeMemUtils.putNativePtr(chunk, lastChunk.rawPtr)
        lastChunk = chunk
        return interpretOpaquePointed(chunk.rawPtr + gapForPointer.toLong())
    }

    @PublishedApi
    internal fun clearImpl() {
        this.executeAllDeferred()

        var chunk = lastChunk
        while (chunk != null) {
            val nextChunk = nativeMemUtils.getNativePtr(chunk)
            parent.free(chunk)
            chunk = interpretNullableOpaquePointed(nextChunk)
        }
    }

}

class Arena(parent: NativeFreeablePlacement = nativeHeap) : ArenaBase(parent) {
    fun clear() = this.clearImpl()
}

/**
 * Allocates variable of given type.
 *
 * @param T must not be abstract
 */
inline fun <reified T : CVariable> NativePlacement.alloc(): T =
        alloc(typeOf<T>()).reinterpret()

@PublishedApi
internal fun NativePlacement.alloc(type: CVariable.Type): NativePointed =
        alloc(type.size, type.align)

/**
 * Allocates variable of given type and initializes it applying given block.
 *
 * @param T must not be abstract
 */
inline fun <reified T : CVariable> NativePlacement.alloc(initialize: T.() -> Unit): T =
        alloc<T>().also { it.initialize() }

/**
 * Allocates C array of given elements type and length.
 *
 * @param T must not be abstract
 */
inline fun <reified T : CVariable> NativePlacement.allocArray(length: Long): CArrayPointer<T> =
        alloc(sizeOf<T>() * length, alignOf<T>()).reinterpret<T>().ptr

/**
 * Allocates C array of given elements type and length.
 *
 * @param T must not be abstract
 */
inline fun <reified T : CVariable> NativePlacement.allocArray(length: Int): CArrayPointer<T> =
        allocArray(length.toLong())

/**
 * Allocates C array of given elements type and length, and initializes its elements applying given block.
 *
 * @param T must not be abstract
 */
inline fun <reified T : CVariable> NativePlacement.allocArray(length: Long,
                                                              initializer: T.(index: Long)->Unit): CArrayPointer<T> {
    val res = allocArray<T>(length)

    (0 .. length - 1).forEach { index ->
        res[index].initializer(index)
    }

    return res
}

/**
 * Allocates C array of given elements type and length, and initializes its elements applying given block.
 *
 * @param T must not be abstract
 */
inline fun <reified T : CVariable> NativePlacement.allocArray(length: Int,
                                                              initializer: T.(index: Int)->Unit): CArrayPointer<T> =
        allocArray(length.toLong()) { index ->
            this.initializer(index.toInt())
        }


/**
 * Allocates C array of pointers to given elements.
 */
fun <T : CPointed> NativePlacement.allocArrayOfPointersTo(elements: List<T?>): CArrayPointer<CPointerVar<T>> {
    val res = allocArray<CPointerVar<T>>(elements.size)
    elements.forEachIndexed { index, value ->
        res[index] = value?.ptr
    }
    return res
}

/**
 * Allocates C array of pointers to given elements.
 */
fun <T : CPointed> NativePlacement.allocArrayOfPointersTo(vararg elements: T?) =
        allocArrayOfPointersTo(listOf(*elements))

/**
 * Allocates C array of given values.
 */
inline fun <reified T : CPointer<*>>
        NativePlacement.allocArrayOf(vararg elements: T?): CArrayPointer<CPointerVarOf<T>> {

    return allocArrayOf(listOf(*elements))
}

/**
 * Allocates C array of given values.
 */
inline fun <reified T : CPointer<*>>
        NativePlacement.allocArrayOf(elements: List<T?>): CArrayPointer<CPointerVarOf<T>> {

    val res = allocArray<CPointerVarOf<T>>(elements.size)
    var index = 0
    while (index < elements.size) {
        res[index] = elements[index]
        ++index
    }
    return res
}

fun NativePlacement.allocArrayOf(elements: ByteArray): CArrayPointer<ByteVar> {
    val result = allocArray<ByteVar>(elements.size)
    nativeMemUtils.putByteArray(elements, result.pointed, elements.size)
    return result
}

fun NativePlacement.allocArrayOf(vararg elements: Float): CArrayPointer<FloatVar> {
    val res = allocArray<FloatVar>(elements.size)
    var index = 0
    while (index < elements.size) {
        res[index] = elements[index]
        ++index
    }
    return res
}

fun <T : CPointed> NativePlacement.allocPointerTo() = alloc<CPointerVar<T>>()

fun <T : CVariable> zeroValue(size: Int, align: Int): CValue<T> = object : CValue<T>() {
    override fun getPointer(scope: AutofreeScope): CPointer<T> {
        val result = scope.alloc(size, align)
        nativeMemUtils.zeroMemory(result, size)
        return interpretCPointer(result.rawPtr)!!
    }

    override val size get() = size
}

inline fun <reified T : CVariable> zeroValue(): CValue<T> =
        zeroValue<T>(sizeOf<T>().toInt(), alignOf<T>())

inline fun <reified T : CVariable> cValue(): CValue<T> = zeroValue<T>()

private fun <T : CPointed> NativePlacement.placeBytes(bytes: ByteArray, align: Int): CPointer<T> {
    val result = this.alloc(size = bytes.size, align = align)
    nativeMemUtils.putByteArray(bytes, result, bytes.size)
    return interpretCPointer(result.rawPtr)!!
}

fun <T : CVariable> CPointed.readValues(size: Int, align: Int): CValues<T> {
    val bytes = ByteArray(size)
    nativeMemUtils.getByteArray(this, bytes, size)

    return object : CValues<T>() {
        override fun getPointer(scope: AutofreeScope): CPointer<T> = scope.placeBytes(bytes, align)
        override val size get() = bytes.size
    }
}

inline fun <reified T : CVariable> T.readValues(count: Int): CValues<T> =
        this.readValues<T>(size = count * sizeOf<T>().toInt(), align = alignOf<T>())

fun <T : CVariable> CPointed.readValue(size: Long, align: Int): CValue<T> {
    val bytes = ByteArray(size.toInt())
    nativeMemUtils.getByteArray(this, bytes, size.toInt())
    return object : CValue<T>() {
        override fun getPointer(scope: AutofreeScope): CPointer<T> = scope.placeBytes(bytes, align)
        override val size get() = bytes.size
    }
}

@PublishedApi internal fun <T : CVariable> CPointed.readValue(type: CVariable.Type): CValue<T> =
    readValue(type.size, type.align)

// Note: can't be declared as property due to possible clash with a struct field.
// TODO: find better name.
inline fun <reified T : CStructVar> T.readValue(): CValue<T> = this.readValue(typeOf<T>())

fun CValue<*>.write(location: NativePtr) {
    // TODO: probably CValue must be redesigned.
    val fakeScope = object : AutofreeScope() {
        var used = false
        override fun alloc(size: Long, align: Int): NativePointed {
            assert(!used)
            used = true
            return interpretPointed<ByteVar>(location)
        }
    }

    this.getPointer(fakeScope)
    assert(fakeScope.used)
}

// TODO: optimize
fun <T : CVariable> CValues<T>.getBytes(): ByteArray = memScoped {
    val result = ByteArray(size)

    nativeMemUtils.getByteArray(
            source = this@getBytes.placeTo(memScope).reinterpret<ByteVar>().pointed,
            dest = result,
            length = result.size
    )

    result
}

/**
 * Calls the [block] with temporary copy if this value as receiver.
 */
inline fun <reified T : CStructVar, R> CValue<T>.useContents(block: T.() -> R): R = memScoped {
    this@useContents.placeTo(memScope).pointed.block()
}

inline fun <reified T : CStructVar> CValue<T>.copy(modify: T.() -> Unit): CValue<T> = useContents {
    this.modify()
    this.readValue()
}

inline fun <reified T : CStructVar> cValue(initialize: T.() -> Unit): CValue<T> =
    zeroValue<T>().copy(modify = initialize)

inline fun <reified T : CVariable> createValues(count: Int, initializer: T.(index: Int) -> Unit) = memScoped {
    val array = allocArray<T>(count, initializer)
    array[0].readValues(count)
}

fun cValuesOf(vararg elements: Byte): CValues<ByteVar> = object : CValues<ByteVar>() {
    override fun getPointer(scope: AutofreeScope) = scope.allocArrayOf(elements)
    override val size get() = 1 * elements.size
}

// TODO: optimize other [cValuesOf] methods:

fun cValuesOf(vararg elements: Short): CValues<ShortVar> =
        createValues(elements.size) { index -> this.value = elements[index] }

fun cValuesOf(vararg elements: Int): CValues<IntVar> =
        createValues(elements.size) { index -> this.value = elements[index] }

fun cValuesOf(vararg elements: Long): CValues<LongVar> =
        createValues(elements.size) { index -> this.value = elements[index] }

fun cValuesOf(vararg elements: Float): CValues<FloatVar> = object : CValues<FloatVar>() {
    override fun getPointer(scope: AutofreeScope) = scope.allocArrayOf(*elements)
    override val size get() = 4 * elements.size
}

fun cValuesOf(vararg elements: Double): CValues<DoubleVar> =
        createValues(elements.size) { index -> this.value = elements[index] }

fun <T : CPointed> cValuesOf(vararg elements: CPointer<T>?): CValues<CPointerVar<T>> =
        createValues(elements.size) { index -> this.value = elements[index] }

fun ByteArray.toCValues() = cValuesOf(*this)
fun ShortArray.toCValues() = cValuesOf(*this)
fun IntArray.toCValues() = cValuesOf(*this)
fun LongArray.toCValues() = cValuesOf(*this)
fun FloatArray.toCValues() = cValuesOf(*this)
fun DoubleArray.toCValues() = cValuesOf(*this)
fun <T : CPointed> Array<CPointer<T>?>.toCValues() = cValuesOf(*this)

fun <T : CPointed> List<CPointer<T>?>.toCValues() = this.toTypedArray().toCValues()

private class CString(val bytes: ByteArray): CValues<ByteVar>() {
    override val size get() = bytes.size + 1

    override fun getPointer(scope: AutofreeScope): CPointer<ByteVar> {
        val result = scope.allocArray<ByteVar>(bytes.size + 1)
        nativeMemUtils.putByteArray(bytes, result.pointed, bytes.size)
        result[bytes.size] = 0.toByte()
        return result
    }
}

/**
 * @return the value of zero-terminated UTF-8-encoded C string constructed from given [kotlin.String].
 */
val String.cstr: CValues<ByteVar>
    get() = CString(encodeToUtf8(this))

/**
 * Convert this list of Kotlin strings to C array of C strings,
 * allocating memory for the array and C strings with given [AutofreeScope].
 */
fun List<String>.toCStringArray(autofreeScope: AutofreeScope): CPointer<CPointerVar<ByteVar>> =
        autofreeScope.allocArrayOf(this.map { it.cstr.getPointer(autofreeScope) })

/**
 * Convert this array of Kotlin strings to C array of C strings,
 * allocating memory for the array and C strings with given [AutofreeScope].
 */
fun Array<String>.toCStringArray(autofreeScope: AutofreeScope): CPointer<CPointerVar<ByteVar>> =
        autofreeScope.allocArrayOf(this.map { it.cstr.getPointer(autofreeScope) })


private class WCString(val chars: CharArray): CValues<UShortVar>() {
    override val size get() = 2 * (chars.size + 1)

    override fun getPointer(scope: AutofreeScope): CPointer<UShortVar> {
        val result = scope.allocArray<UShortVar>(chars.size + 1)
        nativeMemUtils.putCharArray(chars, result.pointed, chars.size)
        // TODO: fix, after KT-29627 is fixed.
        nativeMemUtils.putShort((result + chars.size)!!.pointed, 0)
        return result
    }
}

val String.wcstr: CValues<UShortVar>
    get() = WCString(this.toCharArray())

/**
 * TODO: should the name of the function reflect the encoding?
 *
 * @return the [kotlin.String] decoded from given zero-terminated UTF-8-encoded C string.
 */
// TODO: optimize
fun CPointer<ByteVar>.toKString(): String {
    val nativeBytes = this

    var length = 0
    while (nativeBytes[length] != 0.toByte()) {
        ++length
    }

    val bytes = ByteArray(length)
    nativeMemUtils.getByteArray(nativeBytes.pointed, bytes, length)
    return decodeFromUtf8(bytes)
}

class MemScope : ArenaBase() {

    val memScope: MemScope
        get() = this

    val <T: CVariable> CValues<T>.ptr: CPointer<T>
        get() = this@ptr.getPointer(this@MemScope)
}

// TODO: consider renaming `memScoped` because it now supports `defer`.

/**
 * Runs given [block] providing allocation of memory
 * which will be automatically disposed at the end of this scope.
 */
inline fun <R> memScoped(block: MemScope.()->R): R {
    val memScope = MemScope()
    try {
        return memScope.block()
    } finally {
        memScope.clearImpl()
    }
}

fun COpaquePointer.readBytes(count: Int): ByteArray {
    val result = ByteArray(count)
    nativeMemUtils.getByteArray(this.reinterpret<ByteVar>().pointed, result, count)
    return result
}
