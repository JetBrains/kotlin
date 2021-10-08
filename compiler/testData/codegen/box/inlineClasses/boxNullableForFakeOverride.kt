// WITH_RUNTIME
// IGNORE_BACKEND: JVM
abstract class C<T> {
    fun foo(v: T?, x: (T) -> Any?) = v?.let { x(it) }
}

@JvmInline
value class V(val value: Any?)

class D : C<V>()

fun box() = D().foo(V("OK")) { it.value } as String
