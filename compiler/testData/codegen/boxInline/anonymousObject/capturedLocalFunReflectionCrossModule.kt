// ISSUE: KT-78537
// NO_CHECK_LAMBDA_INLINING
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: 2.2_JS
// ^^^ KT-78537 is fixed in 2.3.0-Beta1
// MODULE: lib
// FILE: 1.kt
inline fun <T> myRun(block: () -> T) = block()

// MODULE: main()(lib)
// FILE: 2.kt
fun box(): String {
    val name = myRun {
        fun OK() = "fail 1"
        val OKref = ::OK
        OKref.name
    }
    return name
}
