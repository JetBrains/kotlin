// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR

// FULL_JDK

import java.nio.CharBuffer

fun box(): String {
    val cb = CharBuffer.wrap("OK")
    cb.position(1)
    val o = cb[0]
    val k = (cb as CharSequence).get(0)
    return o.toString() + k
}
