// SKIP_TXT
// FILE: Generic.java
public class Generic<T> {
    public static class ML<E> {}
    public static Generic create() { return null; }
    public <E> void foo(ML<E> w) { }
}

// FILE: main.kt
import Generic.ML

fun main(w: ML<String>) {
    val generic1 = Generic.create()
    val generic2 = Generic.create() ?: return

    // Not enough information to infer E (both K1 and K2 after KT-41794 is done)
    // Because generic information is erased from the raw type scope of `generic1`
    // But the parameter E is still there (that is a questionable behavior)
    generic1.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>(w)

    // `generic2` does have just non-raw type `Generic<Any!>..Generic<*>?`
    generic2.foo(w) // OK in K1, fails in K2 after KT-41794 is done
}
