package org.jetbrains.kotlin.native.interop.indexer

import clang.CXIdxDeclInfo
import kotlin_native.interop.NativePtr

/**
 * This object helps to create native callbacks with signature `void (CXClientData, const CXIdxDeclInfo *)`.
 * At any time only one instance of native callback can exist.
*/
internal object indexDeclarationCallback {

    /**
     * The Kotlin function to be called when native callback is called.
     */
    private var function: ((CXIdxDeclInfo)->Unit)? = null

    /**
     * Creates instance of native callback with given Kotlin implementation.
     *
     * To create another one, this instance must be destroyed using [reset].
     */
    fun setUp(func: (CXIdxDeclInfo)->Unit): NativePtr {
        assert (function == null)
        function = func
        return nativeCallbackPtr
    }

    /**
     * Destroys the (only) instance of native callback.
     */
    fun reset() {
        assert (function != null)
        function = null
    }

    /**
     * This function is called from native callback implementation.
     */
    @JvmStatic
    private fun entryFromNative(info: Long) {
        function!!(CXIdxDeclInfo(NativePtr.Companion.byValue(info)!!))
    }

    private val nativeCallbackPtr: NativePtr

    init {
        System.loadLibrary("callback")
        nativeCallbackPtr = NativePtr.byValue(nativeCallbackPtr())!!
    }

    /**
     * Returns the pointer to native callback.
     */
    private external fun nativeCallbackPtr(): Long
}