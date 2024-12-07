// TARGET_BACKEND: NATIVE

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import kotlinx.cinterop.*

fun getNp() : NativePtr? = null
fun getOp() : COpaquePointer? = null

fun box() : String {
    if ((null as NativePtr?) != null) return "FAIL 1.1"
    if ((null as? NativePtr) != null) return "FAIL 1.2"
    if (null !is NativePtr?) return "FAIL 1.3"
    if (null is NativePtr) return "FAIL 1.4"

    if ((null as COpaquePointer?) != null) return "FAIL 2.1"
    if ((null as? COpaquePointer) != null) return "FAIL 2.2"
    if (null !is COpaquePointer?) return "FAIL 2.3"
    if (null is COpaquePointer) return "FAIL 2.4"

    if ((getNp() as NativePtr?) != null) return "FAIL 3.1"
    if ((getNp() as? NativePtr) != null) return "FAIL 3.2"
    if (getNp() !is NativePtr?) return "FAIL 3.3"
    if (getNp() is NativePtr) return "FAIL 3.4"

    if ((getOp() as COpaquePointer?) != null) return "FAIL 4.1"
    if ((getOp() as? COpaquePointer) != null) return "FAIL 4.2"
    if (getOp() !is COpaquePointer?) return "FAIL 4.3"
    if (getOp() is COpaquePointer) return "FAIL 4.4"

    return "OK"
}