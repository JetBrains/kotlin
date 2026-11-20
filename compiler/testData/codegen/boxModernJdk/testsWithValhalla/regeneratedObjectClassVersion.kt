// ISSUE: KT-89992
// WITH_STDLIB

import kotlin.coroutines.*

// The stdlib's `Continuation` function regenerates an anonymous object compiled for Java 8.
fun box(): String {
    val continuation = Continuation<Unit>(EmptyCoroutineContext) {}
    val file = continuation.javaClass.name.substringAfterLast('.') + ".class"
    val bytes = continuation.javaClass.getResourceAsStream(file)!!.readBytes()
    val minor = ((bytes[4].toInt() and 0xFF) shl 8) or (bytes[5].toInt() and 0xFF)
    val major = ((bytes[6].toInt() and 0xFF) shl 8) or (bytes[7].toInt() and 0xFF)
    return if (minor == 0xFFFF && major == 72) "OK" else "Fail: $file has version $major.$minor"
}
