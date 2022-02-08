// TARGET_BACKEND: JVM
// FILE: I.java
public interface I<T> {
    public T create();
}

// FILE: box.kt
// A specific bytecode pattern here may confuse POP propagation.
inline fun <reified T, V : Any> I<V>.bar(default: T, crossinline baz: V.(T) -> T) =
    u@{ it: Any? -> create().baz(it as? T ?: return@u default) }

fun box() = I<String> { "O" }.bar("fail") { this + it }("K")
