// SKIP_TXT
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE
// FILE: Generic.java

import java.util.List;

public class Generic<T> {

    public static class ML<E> {}
    public static Generic create() { return null; }

    public String[] getFoo()
}

// FILE: main.kt
fun main() {
    val generic = Generic.create()

    for (x in generic.foo) {
        x.length // Arrays don't become raw
    }

    for (x in generic.getFoo()) {
        x.length // Arrays don't become raw
    }
}
