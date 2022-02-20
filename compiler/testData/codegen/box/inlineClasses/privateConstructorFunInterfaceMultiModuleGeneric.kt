// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// IGNORE_BACKEND: JVM
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter
// MODULE: lib
// FILE: lib.kt

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z<T> private constructor(private val value: T) {
    fun result(): String = value as String

    companion object {
        fun create(value: Any?): Z<Any?> = Z(value)
    }
}

fun interface IFoo<T> {
    fun foo(x: T): String
}

fun foo1(fs: IFoo<Z<Any?>>) = fs.foo(Z.create("OK"))

// MODULE: main(lib)
// FILE: main.kt

fun box(): String =
    foo1 { it.result() }
