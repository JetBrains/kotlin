package org.jetbrains.kotlin.native.interop.indexer

import clang.CXIdxDeclInfo
import kotlin_native.interop.NativePtr

internal object indexDeclarationCallback {

    private var function: ((CXIdxDeclInfo)->Unit)? = null

    fun setUp(func: (CXIdxDeclInfo)->Unit): NativePtr {
        assert (function == null)
        function = func
        return nativeCallbackPtr
    }

    fun reset() {
        assert (function != null)
        function = null
    }

    @JvmStatic
    private fun entryFromNative(info: Long) {
        function!!(CXIdxDeclInfo(NativePtr.Companion.byValue(info)!!))
    }

    private val nativeCallbackPtr: NativePtr

    init {
        System.loadLibrary("callback")
        nativeCallbackPtr = NativePtr.byValue(nativeCallbackPtr())!!
    }

    private external fun nativeCallbackPtr(): Long
}