// IGNORE_BACKEND_FIR: JVM_IR
// Note: does not pass on FIR because of non-prohibited Kotlin synthetic properties,
// fun getS() = s is considered to be recursive here :(
// It's a question to be discussed in Dec 2019. Muted at this moment.
// WITH_RUNTIME
// FILE: lateinit.kt
private lateinit var s: String

object C {
    fun setS(value: String) { s = value }
    fun getS() = s
}

// FILE: test.kt
import kotlin.UninitializedPropertyAccessException

fun box(): String {
    var str2: String = ""
    try {
        str2 = C.getS()
        return "Should throw an exception"
    }
    catch (e: UninitializedPropertyAccessException) {
        return "OK"
    }
    catch (e: Throwable) {
        return "Unexpected exception: ${e::class}"
    }

}
