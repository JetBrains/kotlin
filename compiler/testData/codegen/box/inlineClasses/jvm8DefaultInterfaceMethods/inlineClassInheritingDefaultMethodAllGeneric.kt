// !JVM_DEFAULT_MODE: all
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// LANGUAGE: +GenericInlineClassParameter

interface I {
    fun foo(): A<String> = A("OK")
}

inline class A<T: String>(val x: T): I { }

fun box() = A("").foo().x
