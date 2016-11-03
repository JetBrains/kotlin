// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// FULL_JDK

import java.nio.CharBuffer

fun box(): String {
    val cb = CharBuffer.wrap("OK")
    cb.position(1)
    val o = cb[0]
    val k = (cb as CharSequence).get(0)
    return o.toString() + k
}
