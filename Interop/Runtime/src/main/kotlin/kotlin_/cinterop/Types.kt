package kotlin_.cinterop

/**
 * The entity which has an associated native pointer.
 * Subtypes are supposed to represent interpretations of the pointed data or code.
 *
 * This interface is likely to be handled by compiler magic and shouldn't be subtyped by arbitrary classes.
 */
interface NativePointed {
    val rawPtr: NativePtr
}

// `null` value of `NativePointed?` is mapped to `nativeNullPtr`.
val NativePointed?.rawPtr: NativePtr
    get() = this?.rawPtr ?: nativeNullPtr

/**
 * Returns interpretation of entity with given pointer, or `null` if it is null.
 *
 * @param T must not be abstract
 */
inline fun <reified T : NativePointed> interpretNullablePointed(ptr: NativePtr): T? {
    return ifNotNull(ptr) {
        interpretPointed<T>(it)
    }
}

/**
 * Applies the function to the pointer if it is not null, otherwise returns `null`.
 */
inline fun <T> ifNotNull(ptr: NativePtr, function: (NativePtr)->T): T? {
    return if (ptr == nativeNullPtr) {
        null
    } else {
        function(ptr)
    }
}

/**
 * Applies the function to the pointer ensuring that it is not null.
 */
inline fun <T> ensuringNotNull(ptr: NativePtr, function: (NativePtr)->T): T {
    if (ptr == nativeNullPtr) {
        throw IllegalArgumentException()
    } else {
        return function(ptr)
    }
}

/**
 * Changes the interpretation of the pointed data or code.
 */
inline fun <reified T : NativePointed> NativePointed.reinterpret(): T = interpretPointed(this.rawPtr)

/**
 * C data or code.
 */
interface CPointed : NativePointed

/**
 * C pointer.
 */
class CPointer<T : CPointed> private constructor(val rawValue: NativePtr) {
    companion object {
        fun <T : CPointed> create(rawValue: NativePtr) = ensuringNotNull(rawValue) {
            CPointer<T>(rawValue)
        }

        fun <T : CPointed> createNullable(rawValue: NativePtr) = ifNotNull(rawValue) {
            CPointer<T>(it)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true // fast path
        }

        return (other is CPointer<*>) && (rawValue == other.rawValue)
    }

    override fun hashCode(): Int {
        return rawValue.hashCode()
    }

    override fun toString(): String {
        val hex = "%x".format(rawValue)
        return "CPointer(raw=0x$hex)"
    }
}

/**
 * Returns the pointer to this data or code.
 */
val <T : CPointed> T.ptr: CPointer<T>
    get() = CPointer.create(this.rawPtr)

/**
 * Returns the corresponding [CPointed].
 *
 * @param T must not be abstract
 */
inline val <reified T : CPointed> CPointer<T>.pointed: T
    get() = interpretPointed<T>(this.rawValue)

// `null` value of `CPointer?` is mapped to `nativeNullPtr`
val CPointer<*>?.rawValue: NativePtr
    get() = this?.rawValue ?: nativeNullPtr

fun <T : CPointed> CPointer<*>.reinterpret() = this as CPointer<T>

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
            assert (size % align == 0L)
        }

        companion object
    }

    companion object {
        inline fun <reified T : CVariable> sizeOf() = Type.of<T>().size
        inline fun <reified T : CVariable> alignOf() = Type.of<T>().align
    }
}

/**
 * The C data which is composed from number of members.
 */
interface CAggregate : CPointed

/**
 * Returns the member of this [CAggregate] which is located by given offset in bytes.
 */
inline fun <reified T : CPointed> CAggregate.memberAt(offset: Long): T {
    return interpretPointed<T>(this.rawPtr + offset)
}

/**
 * The C struct-typed variable located in memory.
 */
abstract class CStructVar : CVariable, CAggregate {
    open class Type(size: Long, align: Int) : CVariable.Type(size, align)
}

/**
 * The C primitive-typed variable located in memory.
 */
sealed class CPrimitiveVar : CVariable {
    // aligning by size is obviously enough
    open class Type(size: Int, align: Int = size) : CVariable.Type(size.toLong(), align)
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
inline var <reified P : CPointer<*>> CPointerVarWithValueMappedTo<P>.value: P?
    get() = CPointer.createNullable<CPointed>(nativeMemUtils.getPtr(this)) as P?
    set(value) = nativeMemUtils.putPtr(this, value.rawValue)

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

class CArray<T : CVariable>(override val rawPtr: NativePtr) : CAggregate

inline fun <reified T : CVariable> CArray<T>.elementOffset(index: Long) = if (index == 0L) {
    0L // optimization for JVM impl which uses reflection for now.
} else {
    index * CVariable.sizeOf<T>()
}

inline operator fun <reified T : CVariable> CArray<T>.get(index: Long): T = memberAt(elementOffset(index))
inline operator fun <reified T : CVariable> CArray<T>.get(index: Int) = this.get(index.toLong())

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
 */
typealias CFunctionPointer<T> = CPointer<CFunction<T>>

/**
 * The variable containing a [CFunctionPointer].
 */
typealias CFunctionPointerVar<T> = CPointerVarWithValueMappedTo<CFunctionPointer<T>>