// WITH_STDLIB
// IGNORE_BACKEND: JVM
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

abstract class C<T> {
    fun foo(v: T?, x: (T) -> Any?) = v?.let { x(it) }
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class V(val value: Any?)

class D : C<V>()

fun box() = D().foo(V("OK")) { it.value } as String
