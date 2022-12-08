// TARGET_BACKEND: JVM
// NO_CHECK_LAMBDA_INLINING
// WITH_STDLIB
// IGNORE_BACKEND: JVM, ANDROID
// IGNORE_BACKEND_MULTI_MODULE: JVM, JVM_IR, JVM_MULTI_MODULE_OLD_AGAINST_IR, JVM_MULTI_MODULE_IR_AGAINST_OLD
// IGNORE_BACKEND_K2_MULTI_MODULE: JVM_IR JVM_IR_SERIALIZE
// IGNORE_INLINER: BYTECODE
// FILE: 1.kt

interface Flow<T> {
    val result: String
}

inline fun <reified T> foo() =
    object {
        fun test() = object : Flow<T> {
            override val result: String = "OK"
        }
    }.test()

// FILE: 2.kt

fun box(): String =
    foo<Any>().result
