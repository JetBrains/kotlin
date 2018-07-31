// FILE: A.kt
// LANGUAGE_VERSION: 1.2
// TODO: Unmute when automatic conversion experimental <-> release will be implemented
// IGNORE_BACKEND: JVM, JS, NATIVE, JVM_IR, JS_IR

suspend fun dummy() = "OK"

suspend fun String.dummy() = this + "K"

suspend fun String.dummy(s: String) = this + s

class C {
    suspend fun dummy() = "OK"
}

class WithNested {
    class Nested {
        suspend fun dummy() = "OK"
    }
}

class WithInner {
    inner class Inner {
        suspend fun dummy() = "OK"
    }
}


// FILE: B.kt
// LANGUAGE_VERSION: 1.3
import kotlin.coroutines.experimental.*

fun box(): String {
//    val continuation = object : Continuation<String> {
//        override val context = EmptyCoroutineContext
//        override fun resume(value: String) {
//        }
//        override fun resumeWithException(exception: Throwable) {
//            throw exception
//        }
//    }
//    if (dummy(continuation) != "OK") return "FAIL 1"
//    if ("O".dummy(continuation) != "OK") return "FAIL 2"
//    if ("O".dummy("K", continuation) != "OK") return "FAIL 3"
//    if (C().dummy(continuation) != "OK") return "FAIL 4"
//    if (WithNested.Nested().dummy(continuation) != "OK") return "FAIL 5"
//    if (WithInner().Inner().dummy(continuation) != "OK") return "FAIL 6"
    return "OK"
}
