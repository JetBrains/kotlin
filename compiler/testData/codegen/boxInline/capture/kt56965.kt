// NO_CHECK_LAMBDA_INLINING
// IGNORE_BACKEND: JVM
// IGNORE_BACKEND_MULTI_MODULE: JVM, JVM_MULTI_MODULE_OLD_AGAINST_IR
// FILE: 1.kt
inline fun foo(block: () -> String) = block()

// FILE: 2.kt
fun box() = foo {
    val s = "O"
    val obj = object {
        fun local() = localInline { it }
        inline fun localInline(block: (String) -> String) = block(s) + "K"
    }
    return obj.local()
}
