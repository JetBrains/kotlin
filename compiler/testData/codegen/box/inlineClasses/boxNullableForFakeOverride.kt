// IGNORE_BACKEND: JVM
abstract class C<T> {
    fun foo(v: T?, x: (T) -> Any?) = v?.let { x(it) }
}

inline class V(val value: Any?)

class D : C<V>()

fun box() = D().foo(V("OK")) { it.value } as String
