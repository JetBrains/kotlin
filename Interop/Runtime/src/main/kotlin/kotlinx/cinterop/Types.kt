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

/**
 * The entity which has an associated native pointer.
 * Subtypes are supposed to represent interpretations of the pointed data or code.
 *
 * This interface is likely to be handled by compiler magic and shouldn't be subtyped by arbitrary classes.
 *
 * TODO: the behavior of [equals], [hashCode] and [toString] differs on Native and JVM backends.
 */
interface NativePointed {
    val rawPtr: NativePtr
}

// `null` value of `NativePointed?` is mapped to `nativeNullPtr`.
val NativePointed?.rawPtr: NativePtr
    get() = if (this != null) this.rawPtr else nativeNullPtr

/**
 * Returns interpretation of entity with given pointer.
 *
 * @param T must not be abstract
 */
inline fun <reified T : NativePointed> interpretPointed(ptr: NativePtr): T = interpretNullablePointed<T>(ptr)!!

/**
 * Changes the interpretation of the pointed data or code.
 */
inline fun <reified T : NativePointed> NativePointed.reinterpret(): T = interpretPointed(this.rawPtr)

/**
 * C data or code.
 */
interface CPointed : NativePointed

/**
 * Represents a reference to (possibly empty) sequence of C values.
 * It can be either a stable pointer [CPointer] or a sequence of immutable values [CValues].
 *
 * [CValuesRef] is designed to be used as Kotlin representation of pointer-typed parameters of C functions.
 * When passing [CPointer] as [CValuesRef] to the Kotlin binding method, the C function receives exactly this pointer.
 * Passing [CValues] has nearly the same semantics as passing by value: the C function receives
 * the pointer to the temporary copy of these values, and the caller can't observe the modifications to this copy.
 * The copy is valid until the C function returns.
 */
abstract class CValuesRef<T : CPointed> {
    /**
     * If this reference is [CPointer], returns this pointer.
     * Otherwise copies the referenced values to [placement] and returns the pointer to the copy.
     */
    abstract fun getPointer(placement: NativePlacement): CPointer<T>
}

/**
 * The (possibly empty) sequence of immutable C values.
 * It is self-contained and doesn't depend on native memory.
 */
abstract class CValues<T : CVariable> : CValuesRef<T>() {
    /**
     * Copies the values to [placement] and returns the pointer to the copy.
     */
    override abstract fun getPointer(placement: NativePlacement): CPointer<T>

    // TODO: optimize
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CValues<*>) return false

        val thisBytes = this.getBytes()
        val otherBytes = other.getBytes()

        if (thisBytes.size != otherBytes.size) {
            return false
        }

        for (index in 0 .. thisBytes.size - 1) {
            if (thisBytes[index] != otherBytes[index]) {
                return false
            }
        }

        return true
    }

    override fun hashCode(): Int {
        var result = 0
        for (byte in this.getBytes()) {
            result = result * 31 + byte
        }
        return result
    }

    abstract val size: Int
}

fun <T : CVariable> CValues<T>.placeTo(placement: NativePlacement) = this.getPointer(placement)

/**
 * The single immutable C value.
 * It is self-contained and doesn't depend on native memory.
 *
 * TODO: consider providing an adapter instead of subtyping [CValues].
 */
abstract class CValue<T : CVariable> : CValues<T>()

/**
 * C pointer.
 */
class CPointer<T : CPointed> internal constructor(val rawValue: NativePtr) : CValuesRef<T>() {

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true // fast path
        }

        return (other is CPointer<*>) && (rawValue == other.rawValue)
    }

    override fun hashCode(): Int {
        return rawValue.hashCode()
    }

    override fun toString() = this.cPointerToString()

    override fun getPointer(placement: NativePlacement) = this
}

/**
 * Returns the pointer to this data or code.
 */
val <T : CPointed> T.ptr: CPointer<T>
    get() = interpretCPointer(this.rawPtr)!!

/**
 * Returns the corresponding [CPointed].
 *
 * @param T must not be abstract
 */
inline val <reified T : CPointed> CPointer<T>.pointed: T
    get() = interpretPointed<T>(this.rawValue)

// `null` value of `CPointer?` is mapped to `nativeNullPtr`
val CPointer<*>?.rawValue: NativePtr
    get() = if (this != null) this.rawValue else nativeNullPtr

fun <T : CPointed> CPointer<*>.reinterpret(): CPointer<T> = interpretCPointer(this.rawValue)!!

/**
 * The [CPointed] without any specified interpretation.
 */
interface COpaque : CPointed // TODO: should it correspond to COpaquePointer?

/**
 * The pointer with an opaque type.
 */
typealias COpaquePointer = CPointer<out CPointed> // FIXME

/**
 * The variable containing a [COpaquePointer].
 */
typealias COpaquePointerVar = CPointerVarWithValueMappedTo<COpaquePointer>

/**
 * The C data variable located in memory.
 *
 * The non-abstract subclasses should represent the (complete) C data type and thus specify size and alignment.
 * Each such subclass must have a companion object which is a [Type].
 */
interface CVariable : CPointed {

    /**
     * The (complete) C data type.
     *
     * @param size the size in bytes of data of this type
     * @param align the alignments in bytes that is enough for this data type.
     * It may be greater than actually required for simplicity.
     */
    open class Type(val size: Long, val align: Int) {

        init {
            require(size % align == 0L)
        }

    }
}

inline fun <reified T : CVariable> sizeOf() = typeOf<T>().size
inline fun <reified T : CVariable> alignOf() = typeOf<T>().align

/**
 * Returns the member of this [CStructVar] which is located by given offset in bytes.
 */
inline fun <reified T : CPointed> CStructVar.memberAt(offset: Long): T {
    return interpretPointed<T>(this.rawPtr + offset)
}

inline fun <reified T : CVariable> CStructVar.arrayMemberAt(offset: Long): CArrayPointer<T> {
    return interpretCPointer<T>(this.rawPtr + offset)!!
}

/**
 * The C struct-typed variable located in memory.
 */
abstract class CStructVar : CVariable {
    open class Type(size: Long, align: Int) : CVariable.Type(size, align)
}

/**
 * The C primitive-typed variable located in memory.
 */
sealed class CPrimitiveVar : CVariable {
    // aligning by size is obviously enough
    open class Type(size: Int) : CVariable.Type(size.toLong(), align = size)
}

interface CEnum {
    val value: Number
}
abstract class CEnumVar : CPrimitiveVar()

// generics below are used for typedef support
// these classes are not supposed to be used directly, instead the typealiases are provided.

class CInt8VarWithValueMappedTo<T : Byte>(override val rawPtr: NativePtr) : CPrimitiveVar() {
    companion object : Type(1)
}

class CInt16VarWithValueMappedTo<T : Short>(override val rawPtr: NativePtr) : CPrimitiveVar() {
    companion object : Type(2)
}

class CInt32VarWithValueMappedTo<T : Int>(override val rawPtr: NativePtr) : CPrimitiveVar() {
    companion object : Type(4)
}

class CInt64VarWithValueMappedTo<T : Long>(override val rawPtr: NativePtr) : CPrimitiveVar() {
    companion object : Type(8)
}

class CFloat32VarWithValueMappedTo<T : Float>(override val rawPtr: NativePtr) : CPrimitiveVar() {
    companion object : Type(4)
}

class CFloat64VarWithValueMappedTo<T : Double>(override val rawPtr: NativePtr) : CPrimitiveVar() {
    companion object : Type(8)
}

typealias CInt8Var = CInt8VarWithValueMappedTo<Byte>
typealias CInt16Var = CInt16VarWithValueMappedTo<Short>
typealias CInt32Var = CInt32VarWithValueMappedTo<Int>
typealias CInt64Var = CInt64VarWithValueMappedTo<Long>
typealias CFloat32Var = CFloat32VarWithValueMappedTo<Float>
typealias CFloat64Var = CFloat64VarWithValueMappedTo<Double>

var <T : Byte> CInt8VarWithValueMappedTo<T>.value: T
    get() = nativeMemUtils.getByte(this) as T
    set(value) = nativeMemUtils.putByte(this, value)

var <T : Short> CInt16VarWithValueMappedTo<T>.value: T
    get() = nativeMemUtils.getShort(this) as T
    set(value) = nativeMemUtils.putShort(this, value)

var <T : Int> CInt32VarWithValueMappedTo<T>.value: T
    get() = nativeMemUtils.getInt(this) as T
    set(value) = nativeMemUtils.putInt(this, value)

var <T : Long> CInt64VarWithValueMappedTo<T>.value: T
    get() = nativeMemUtils.getLong(this) as T
    set(value) = nativeMemUtils.putLong(this, value)

// TODO: ensure native floats have the appropriate binary representation

var <T : Float> CFloat32VarWithValueMappedTo<T>.value: T
    get() = nativeMemUtils.getFloat(this) as T
    set(value) = nativeMemUtils.putFloat(this, value)

var <T : Double> CFloat64VarWithValueMappedTo<T>.value: T
    get() = nativeMemUtils.getDouble(this) as T
    set(value) = nativeMemUtils.putDouble(this, value)


class CPointerVarWithValueMappedTo<T : CPointer<*>>(override val rawPtr: NativePtr) : CVariable {
    companion object : CVariable.Type(pointerSize.toLong(), pointerSize)
}

/**
 * The C data variable containing the pointer to `T`.
 */
typealias CPointerVar<T> = CPointerVarWithValueMappedTo<CPointer<T>>

/**
 * The value of this variable.
 */
inline var <P : CPointer<*>> CPointerVarWithValueMappedTo<P>.value: P?
    get() = interpretCPointer<CPointed>(nativeMemUtils.getNativePtr(this)) as P?
    set(value) = nativeMemUtils.putNativePtr(this, value.rawValue)

/**
 * The code or data pointed by the value of this variable.
 * 
 * @param T must not be abstract
 */
inline var <reified T : CPointed, reified P : CPointer<T>> CPointerVarWithValueMappedTo<P>.pointed: T?
    get() = this.value?.pointed
    set(value) {
        this.value = value?.ptr as P?
    }

inline operator fun <reified T : CVariable> CPointer<T>.get(index: Long): T {
    val offset = if (index == 0L) {
        0L // optimization for JVM impl which uses reflection for now.
    } else {
        index * sizeOf<T>()
    }
    return interpretPointed(this.rawValue + offset)
}

inline operator fun <reified T : CVariable> CPointer<T>.get(index: Int): T = this.get(index.toLong())

typealias CArrayPointer<T> = CPointer<T>
typealias CArrayPointerVar<T> = CPointerVar<T>

/**
 * The type of C function.
 */
interface CFunctionType

/**
 * The type of C function constructed from some Kotlin function, possibly using an adapter.
 * The (non-abstract) implementation classes are supposed to be object declarations.
 */
interface CAdaptedFunctionType<F : Function<*>> : CFunctionType {

    /**
     * Returns a raw pointer to C function of this type, which calls given Kotlin *static* function.
     *
     * This inconvenient method should not be used directly; use [staticCFunction] instead.
     *
     * @param function must be *static*, i.e. an (unbound) reference to a Kotlin function or
     * a closure which doesn't capture any variable
     */
    fun fromStatic(function: F): NativePtr

    companion object
}

/**
 * Returns a pointer to `T`-typed C function which calls given Kotlin *static* function.
 * @see CAdaptedFunctionType.fromStatic
 */
inline fun <reified F : Function<*>, reified T : CAdaptedFunctionType<F>> staticCFunction(body: F): CFunctionPointer<T> {
    val type = CAdaptedFunctionType.getInstanceOf<T>()
    return interpretPointed<CFunction<T>>(type.fromStatic(body)).ptr
}

/**
 * The C function.
 */
class CFunction<T : CFunctionType>(override val rawPtr: NativePtr) : CPointed

/**
 * The pointer to [CFunction].
 * TODO: remove.
 */
typealias CFunctionPointer<T> = CPointer<CFunction<T>>

/**
 * The variable containing a [CFunctionPointer].
 * TODO: remove.
 */
typealias CFunctionPointerVar<T> = CPointerVarWithValueMappedTo<CFunctionPointer<T>>