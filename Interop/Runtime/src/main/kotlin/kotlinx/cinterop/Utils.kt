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

// TODO: implement optimally
class Arena(private val parent: NativeFreeablePlacement = nativeHeap) : NativePlacement {

    private val allocatedChunks = ArrayList<NativePointed>()

    override fun alloc(size: Long, align: Int): NativePointed {
        val res = parent.alloc(size, align)
        try {
            allocatedChunks.add(res)
            return res
        } catch (e: Throwable) {
            parent.free(res)
            throw e
        }
    }

    fun clear() {
        allocatedChunks.forEach {
            parent.free(it)
        }

        allocatedChunks.clear()
    }

}

/**
 * Allocates variable of given type.
 *
 * @param T must not be abstract
 */
inline fun <reified T : CVariable> NativePlacement.alloc(): T =
        alloc(sizeOf<T>(), alignOf<T>()).reinterpret()

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
    override fun getPointer(placement: NativePlacement): CPointer<T> {
        val result = placement.alloc(size, align)
        nativeMemUtils.zeroMemory(result, size)
        return interpretCPointer(result.rawPtr)!!
    }

    override val size get() = size
}

inline fun <reified T : CVariable> zeroValue(): CValue<T> =
        zeroValue<T>(sizeOf<T>().toInt(), alignOf<T>())

private fun <T : CPointed> NativePlacement.placeBytes(bytes: ByteArray, align: Int): CPointer<T> {
    val result = this.alloc(size = bytes.size, align = align)
    nativeMemUtils.putByteArray(bytes, result, bytes.size)
    return interpretCPointer(result.rawPtr)!!
}

fun <T : CVariable> CPointed.readValues(size: Int, align: Int): CValues<T> {
    val bytes = ByteArray(size)
    nativeMemUtils.getByteArray(this, bytes, size)

    return object : CValues<T>() {
        override fun getPointer(placement: NativePlacement): CPointer<T> = placement.placeBytes(bytes, align)
        override val size get() = bytes.size
    }
}

inline fun <reified T : CVariable> T.readValues(count: Int): CValues<T> =
        this.readValues<T>(size = count * sizeOf<T>().toInt(), align = alignOf<T>())

fun <T : CVariable> CPointed.readValue(size: Long, align: Int): CValue<T> {
    val bytes = ByteArray(size.toInt())
    nativeMemUtils.getByteArray(this, bytes, size.toInt())
    return object : CValue<T>() {
        override fun getPointer(placement: NativePlacement): CPointer<T> = placement.placeBytes(bytes, align)
        override val size get() = bytes.size
    }
}

// Note: can't be declared as property due to possible clash with a struct field.
// TODO: find better name.
inline fun <reified T : CStructVar> T.readValue(): CValue<T> = this.readValue(sizeOf<T>(), alignOf<T>())

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
    override fun getPointer(placement: NativePlacement) = placement.allocArrayOf(elements)
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
    override fun getPointer(placement: NativePlacement) = placement.allocArrayOf(*elements)
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

/**
 * @return the value of zero-terminated UTF-8-encoded C string constructed from given [kotlin.String].
 */
val String.cstr: CValues<ByteVar>
    get() {
        val bytes = encodeToUtf8(this)

        return object : CValues<ByteVar>() {
            override val size get() = bytes.size + 1

            override fun getPointer(placement: NativePlacement): CPointer<ByteVar> {
                val result = placement.allocArray<ByteVar>(bytes.size + 1)
                nativeMemUtils.putByteArray(bytes, result.pointed, bytes.size)
                result[bytes.size] = 0.toByte()
                return result
            }
        }
    }

val String.wcstr: CValues<ShortVar>
    get() {
        val chars = CharArray(this.length, { i -> this.get(i)})
        return object : CValues<ShortVar>() {
            override val size get() = 2 * (chars.size + 1)

            override fun getPointer(placement: NativePlacement): CPointer<ShortVar> {
                val result = placement.allocArray<ShortVar>(chars.size + 1)
                nativeMemUtils.putCharArray(chars, result.pointed, chars.size)
                result[chars.size] = 0.toShort()
                return result
            }
        }
    }

/**
 * TODO: should the name of the function reflect the encoding?
 *
 * @return the [kotlin.String] decoded from given zero-terminated UTF-8-encoded C string.
 */
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

class MemScope : NativePlacement {

    private val arena = Arena()

    override fun alloc(size: Long, align: Int) = arena.alloc(size, align)

    fun clear() = arena.clear()

    val memScope: NativePlacement
        get() = this
}

/**
 * Runs given [block] providing allocation of memory
 * which will be automatically disposed at the end of this scope.
 */
inline fun <R> memScoped(block: MemScope.()->R): R {
    val memScope = MemScope()
    try {
        return memScope.block()
    } finally {
        memScope.clear()
    }
}
