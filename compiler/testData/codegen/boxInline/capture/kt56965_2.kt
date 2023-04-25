// NO_CHECK_LAMBDA_INLINING
// IGNORE_BACKEND: JVM
// IGNORE_BACKEND_MULTI_MODULE: JVM, JVM_MULTI_MODULE_IR_AGAINST_OLD
// FILE: 1.kt
inline fun foo(block: () -> String) = block()

inline fun bar() = foo {
    val s = "O"
    val obj = object {
        fun local() = localInline { it }
        inline fun localInline(block: (String) -> String) = block(s) + "K"
    }
    return obj.local()
}

// FILE: 2.kt
fun box() = bar()