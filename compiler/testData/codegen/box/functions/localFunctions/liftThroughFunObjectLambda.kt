// NO_CHECK_LAMBDA_INLINING

// FILE: lib.kt
inline fun <R> myRun(block: () -> R): R {
    return block()
}

// FILE: main.kt
fun box() = myRun {
    object {
        fun foo(): String {
            fun localFun() = "OK"
            return localFun()
        }
    }.foo()
}
