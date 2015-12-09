// FILE: CharBuffer.java

public class CharBuffer implements CharSequence {
    public final int length() {
        return 0;
    }

    public final char charAt(int index) {
        return get(position() + checkIndex(index, 1));
    }

    // The key problem here is that `get` has the same signature as kotlin.CharSequence.get but completely different semantics
    public abstract char get(int index);
    public abstract CharBuffer subSequence(int start, int end);
}

// FILE: main.kt

fun test(cb: CharBuffer) {
    cb.get(0)
    (cb as CharSequence).get(0)
}
