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
        fun create(any: Any) = fromValue(createStablePointer(any))

        /**
         * Creates [StableObjPtr] from given raw value.
         *
         * @param value must be a [value] of some [StableObjPtr]
         */
        fun fromValue(value: COpaquePointer) = StableObjPtr(value)
    }

    /**
     * Disposes the handle. It must not be [used][get] after that.
     */
    fun dispose() {
        disposeStablePointer(value)
    }

    /**
     * Returns the object this handle was [created][create] for.
     */
    fun get(): Any = derefStablePointer(value)

}

@SymbolName("Kotlin_Interop_createStablePointer")
private external fun createStablePointer(any: Any): COpaquePointer

@SymbolName("Kotlin_Interop_disposeStablePointer")
private external fun disposeStablePointer(pointer: COpaquePointer)

@SymbolName("Kotlin_Interop_derefStablePointer")
private external fun derefStablePointer(pointer: COpaquePointer): Any
