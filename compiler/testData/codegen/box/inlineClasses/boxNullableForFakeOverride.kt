// WITH_STDLIB
// IGNORE_BACKEND: JVM
abstract class C<T> {
    fun foo(v: T?, x: (T) -> Any?) = v?.let { x(it) }
}

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class V(val value: Any?)

class D : C<V>()

fun box() = D().foo(V("OK")) { it.value } as String
