package kotlin_native.interop

/**
 * This class provides a way to create a stable handle to any Kotlin object.
 * Its [value] can be safely passed to native code e.g. to be received in a Kotlin callback.
 *
 * Any [StableObjPtr] should be manually [disposed][dispose]
 */
data class StableObjPtr private constructor(val value: NativePtr) {

    companion object {

        /**
         * Creates a handle for given object.
         */
        fun create(any: Any) = StableObjPtr(NativePtr.byValue(newGlobalRef(any))!!)

        /**
         * Creates [StableObjPtr] from given raw value.
         *
         * @param value must be a [value] of some [StableObjPtr]
         */
        fun fromValue(value: NativePtr) = StableObjPtr(value)

        init {
            loadCallbacksLibrary()
        }
    }

    /**
     * Disposes the handle. It must not be [used][get] after that.
     */
    fun dispose() {
        deleteGlobalRef(value.value)
    }

    /**
     * Returns the object this handle was [created][create] for.
     */
    fun get(): Any = derefGlobalRef(value.value)

}

/**
 * Describes the type of native function.
 *
 * The instances of this class are supposed to be Kotlin objects (singletons),
 * because creating the instance implies allocating some amount of non-freeable memory for the instance itself
 * and for any unique Kotlin function "converted" to this type.
 *
 * Native function type definition consists in the following:
 * -   Definitions of native function's parameter and return types to be passed into the constructor
 * -   Implementation of [invoke] method which describes how to convert between these types and Kotlin types used in [F]
 *
 * @param F Kotlin function type corresponding to given native function type
*/
abstract class NativeFunctionType<F : Function<*>> protected constructor(returnType: CType, vararg paramTypes: CType) {

    /**
     * Returns a native function of this type, which calls given Kotlin *static* function.
     *
     * Given function must be *static*, i.e. an (unbound) reference to a Kotlin function or
     * a closure which doesn't capture any variable
     */
    fun fromStatic(function: F): NativePtr {
        // TODO: optimize synchronization
        synchronized(cache) {
            return cache.getOrPut(function, { createFromStatic(function) })
        }
    }

    /**
     * Describes the C type of a function's parameter or return value.
     * It is supposed to be constructed using the primitive types (such as [SInt32]) and the [Struct] combinator.
     *
     * This description omits the details that are irrelevant for the ABI.
     */
    protected open class CType internal constructor(val ffiType: ffi_type) {
        internal constructor(ffiTypePtr: Long) : this(NativePtr.byValue(ffiTypePtr).asRef(ffi_type)!!)
    }

    protected object Void    : CType(ffiTypeVoid())
    protected object UInt8   : CType(ffiTypeUInt8())
    protected object SInt8   : CType(ffiTypeSInt8())
    protected object UInt16  : CType(ffiTypeUInt16())
    protected object SInt16  : CType(ffiTypeSInt16())
    protected object UInt32  : CType(ffiTypeUInt32())
    protected object SInt32  : CType(ffiTypeSInt32())
    protected object UInt64  : CType(ffiTypeUInt64())
    protected object SInt64  : CType(ffiTypeSInt64())
    protected object Pointer : CType(ffiTypePointer())

    protected class Struct(vararg elementTypes: CType) : CType(
            ffiTypeStruct(
                    elementTypes.map { it.ffiType }
            )
    )

    /**
     * This method should invoke given Kotlin function.
     *
     * @param args array of pointers to arguments to be passed to [function]
     * @param ret pointer to memory to be filled with return value of [function]
     */
    protected abstract fun invoke(function: F, args: NativeArray<NativePtrBox>, ret: NativePtr)

    companion object {
        init {
            loadCallbacksLibrary()
        }
    }

    private val ffiCif = ffiCreateCif(returnType.ffiType, paramTypes.map { it.ffiType })

    /**
     * Allocates a native function of this type for given Kotlin function.
     */
    private fun createFromStatic(function: F): NativePtr {
        if (!isStatic(function)) {
            throw IllegalArgumentException()
        }

        val impl = { ret: NativePtr, args: NativeArray<NativePtrBox> ->
            invoke(function, args, ret)
        }

        return ffiCreateClosure(ffiCif, impl)
    }

    /**
     * Returns `true` if given function is *static* as defined in [fromStatic].
     */
    private fun isStatic(function: Function<*>): Boolean {
        // TODO: revise
        try {
            with(function.javaClass.getDeclaredField("INSTANCE")) {
                if (!java.lang.reflect.Modifier.isStatic(modifiers) || !java.lang.reflect.Modifier.isFinal(modifiers)) {
                    return false
                }

                isAccessible = true // TODO: undo

                return get(null) == function
            }
        } catch (e: NoSuchFieldException) {
            return false
        }
    }

    private val cache = mutableMapOf<F, NativePtr>()
}

/**
 * @see NativeFunctionType.fromStatic
 */
fun <F : Function<*>> F.staticAsNative(type: NativeFunctionType<F>) = type.fromStatic(this)

/**
 * Describes a "struct" with native function pointer field.
 */
class NativeFunctionBox<F : Function<*>>(ptr: NativePtr, private val type: NativeFunctionType<F>) : NativeRef(ptr) {

    /**
     * Sets the function pointer field to null or native function calling given Kotlin function.
     */
    fun setStatic(function: F?) {
        val nativeFunPtr = function?.staticAsNative(type)
        bridge.putPtr(ptr, nativeFunPtr)
    }
}

val <F : Function<*>> NativeFunctionType<F>.ref: NativeRef.TypeWithSize<NativeFunctionBox<F>>
    get() = NativeRef.TypeWithSize(8, { NativeFunctionBox(it, this) }) // TODO: 64-bit specific


private fun loadCallbacksLibrary() {
    System.loadLibrary("callbacks")
}


/**
 * Reference to `ffi_type` struct instance.
 */
internal class ffi_type (ptr: NativePtr) : NativeRef(ptr) {
    companion object : Type<ffi_type>(::ffi_type)
}

/**
 * Reference to `ffi_cif` struct instance.
 */
internal class ffi_cif (ptr: NativePtr) : NativeRef(ptr) {
    companion object : Type<ffi_cif>(::ffi_cif)
}

private external fun ffiTypeVoid(): Long
private external fun ffiTypeUInt8(): Long
private external fun ffiTypeSInt8(): Long
private external fun ffiTypeUInt16(): Long
private external fun ffiTypeSInt16(): Long
private external fun ffiTypeUInt32(): Long
private external fun ffiTypeSInt32(): Long
private external fun ffiTypeUInt64(): Long
private external fun ffiTypeSInt64(): Long
private external fun ffiTypePointer(): Long

private external fun ffiTypeStruct0(elements: Long): Long

/**
 * Allocates and initializes `ffi_type` describing the struct.
 *
 * @param elements types of the struct elements
 */
private fun ffiTypeStruct(elementTypes: List<ffi_type>): ffi_type {
    val elements = mallocNativeArrayOf(ffi_type, *elementTypes.toTypedArray(), null).ptr
    val res = ffiTypeStruct0(elements.value)
    if (res == 0L) {
        throw OutOfMemoryError()
    }
    return NativePtr.byValue(res).asRef(ffi_type)!!
}

private external fun ffiCreateCif0(nArgs: Int, rType: Long, argTypes: Long): Long

/**
 * Creates and prepares an `ffi_cif`.
 *
 * @param returnType native function return value type
 * @param paramTypes native function parameter types
 *
 * @return the initialized `ffi_cif`
 */
private fun ffiCreateCif(returnType: ffi_type, paramTypes: List<ffi_type>): ffi_cif {
    val nArgs = paramTypes.size
    val rType = returnType.ptr
    val argTypes = mallocNativeArrayOf(ffi_type, *paramTypes.toTypedArray(), null).ptr
    val res = ffiCreateCif0(nArgs, rType.value, argTypes.value)

    when (res) {
        0L -> throw OutOfMemoryError()
        -1L -> throw Error("FFI_BAD_TYPEDEF")
        -2L -> throw Error("FFI_BAD_ABI")
        -3L -> throw Error("libffi error occurred")
    }

    return NativePtr.byValue(res).asRef(ffi_cif)!!
}

private fun ffiFunImpl0(ffiCif: Long, ret: Long, args: Long, userData: Any) {
    ffiFunImpl(NativePtr.byValue(ffiCif).asRef(ffi_cif)!!,
            NativePtr.byValue(ret)!!,
            NativePtr.byValue(args).asRef(array(NativePtrBox))!!,
            userData as (ret: NativePtr, args: NativeArray<NativePtrBox>) -> Unit)
}

/**
 * This function is called from native code when a native function created with [ffiCreateClosure] is invoked.
 *
 * @param ret pointer to memory to be filled with return value of the invoked native function
 * @param args pointer to array of pointers to arguments passed to the invoked native function
 */
private fun ffiFunImpl(ffiCif: ffi_cif, ret: NativePtr, args: NativeArray<NativePtrBox>,
                       userData: (NativePtr, NativeArray<NativePtrBox>) -> Unit) {

    userData.invoke(ret, args)
}

private external fun ffiCreateClosure0(ffiCif: Long, userData: Any): Long

/**
 * Uses libffi to allocate a native function which will call [ffiFunImpl] when invoked.
 *
 * @param ffiCif describes the type of the function to create
 */
private fun ffiCreateClosure(ffiCif: ffi_cif, userData: (NativePtr, NativeArray<NativePtrBox>) -> Unit): NativePtr {
    val res = ffiCreateClosure0(ffiCif.ptr.value, userData)

    when (res) {
        0L -> throw OutOfMemoryError()
        -1L -> throw Error("libffi error occurred")
    }

    return NativePtr.byValue(res)!!
}

private external fun newGlobalRef(any: Any): Long
private external fun derefGlobalRef(ref: Long): Any
private external fun deleteGlobalRef(ref: Long)