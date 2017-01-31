package kotlinx.cinterop

/**
 * This class provides a way to create a stable handle to any Kotlin object.
 * Its [value] can be safely passed to native code e.g. to be received in a Kotlin callback.
 *
 * Any [StableObjPtr] should be manually [disposed][dispose]
 */
data class StableObjPtr private constructor(val value: COpaquePointer) {

    companion object {

        /**
         * Creates a handle for given object.
         */
        fun create(any: Any) = fromValue(newGlobalRef(any))

        private fun fromValue(value: NativePtr) = fromValue(CPointer.create(value))

        /**
         * Creates [StableObjPtr] from given raw value.
         *
         * @param value must be a [value] of some [StableObjPtr]
         */
        fun fromValue(value: COpaquePointer) = StableObjPtr(value)

        init {
            loadCallbacksLibrary()
        }
    }

    /**
     * Disposes the handle. It must not be [used][get] after that.
     */
    fun dispose() {
        deleteGlobalRef(value.rawValue)
    }

    /**
     * Returns the object this handle was [created][create] for.
     */
    fun get(): Any = derefGlobalRef(value.rawValue)

}

/**
 * Describes the type of C function with adapter for Kotlin functions.
 *
 * The instances of this class are supposed to be Kotlin object declarations (singletons),
 * because it is required by [CAdaptedFunctionType] and
 * because creating the instance implies allocating some amount of non-freeable memory for the instance itself
 * and for any unique Kotlin function "converted" to this type.
 *
 * Native function type definition consists in the following:
 * -   Definitions of native function's parameter and return types to be passed into the constructor
 * -   Implementation of [invoke] method which describes how to convert between these types and Kotlin types used in [F]
 *
 * @param F Kotlin function type corresponding to given native function type
 */
abstract class CAdaptedFunctionTypeImpl<F : Function<*>>
        protected constructor(returnType: CType, vararg paramTypes: CType) : CAdaptedFunctionType<F> {

    override fun fromStatic(function: F): NativePtr {
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
        internal constructor(ffiTypePtr: Long) : this(interpretPointed<ffi_type>(ffiTypePtr))
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
    protected abstract fun invoke(function: F, args: CArray<COpaquePointerVar>, ret: COpaquePointer)

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

        val impl: UserData = { ret: COpaquePointer, args: CArray<COpaquePointerVar> ->
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

private typealias UserData = (ret: COpaquePointer, args: CArray<COpaquePointerVar>)->Unit

private fun loadCallbacksLibrary() {
    System.loadLibrary("callbacks")
}


/**
 * Reference to `ffi_type` struct instance.
 */
internal class ffi_type(override val rawPtr: NativePtr) : COpaque

/**
 * Reference to `ffi_cif` struct instance.
 */
internal class ffi_cif(override val rawPtr: NativePtr) : COpaque

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
    val elements = nativeHeap.allocArrayOfPointersTo(*elementTypes.toTypedArray(), null)
    val res = ffiTypeStruct0(elements.rawPtr)
    if (res == 0L) {
        throw OutOfMemoryError()
    }
    return interpretPointed(res)
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
    val argTypes = nativeHeap.allocArrayOfPointersTo(*paramTypes.toTypedArray(), null)
    val res = ffiCreateCif0(nArgs, returnType.rawPtr, argTypes.rawPtr)

    when (res) {
        0L -> throw OutOfMemoryError()
        -1L -> throw Error("FFI_BAD_TYPEDEF")
        -2L -> throw Error("FFI_BAD_ABI")
        -3L -> throw Error("libffi error occurred")
    }

    return interpretPointed(res)
}

private fun ffiFunImpl0(ffiCif: Long, ret: Long, args: Long, userData: Any) {
    ffiFunImpl(interpretPointed(ffiCif),
            CPointer.create(ret),
            interpretPointed(args),
            userData as UserData)
}

/**
 * This function is called from native code when a native function created with [ffiCreateClosure] is invoked.
 *
 * @param ret pointer to memory to be filled with return value of the invoked native function
 * @param args pointer to array of pointers to arguments passed to the invoked native function
 */
private fun ffiFunImpl(ffiCif: ffi_cif, ret: COpaquePointer, args: CArray<COpaquePointerVar>,
                       userData: UserData) {

    userData.invoke(ret, args)
}

private external fun ffiCreateClosure0(ffiCif: Long, userData: Any): Long

/**
 * Uses libffi to allocate a native function which will call [ffiFunImpl] when invoked.
 *
 * @param ffiCif describes the type of the function to create
 */
private fun ffiCreateClosure(ffiCif: ffi_cif, userData: UserData): NativePtr {
    val res = ffiCreateClosure0(ffiCif.rawPtr, userData)

    when (res) {
        0L -> throw OutOfMemoryError()
        -1L -> throw Error("libffi error occurred")
    }

    return res
}

private external fun newGlobalRef(any: Any): Long
private external fun derefGlobalRef(ref: Long): Any
private external fun deleteGlobalRef(ref: Long)