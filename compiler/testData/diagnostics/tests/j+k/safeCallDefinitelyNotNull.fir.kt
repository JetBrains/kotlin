// SKIP_TXT
// FILE: Api.java
import org.jetbrains.annotations.NotNull;
public abstract class Api<T> {
    public abstract void typeOf(@NotNull T node);
}

// FILE: main.kt
fun <E> foo(a: Api<E>, e: E?) {
    e?.let { a.typeOf(e) }
}
