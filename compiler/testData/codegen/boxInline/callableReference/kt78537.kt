// ISSUE: KT-78537
// NO_CHECK_LAMBDA_INLINING

// FILE: 1.kt
inline fun <T> myRun(block: () -> T) = block()

// FILE: 2.kt
fun box(): String {
    val name = myRun {
        fun OK() = "fail 1"
        val OKref = ::OK
        OKref.name
    }
    return name
}
