// FIR_IDENTICAL
// JSPECIFY_STATE: strict
// ISSUE: KT-83849

// FILE: Box.java
public class Box<V> {
    public void consume(V value) {}
}

// FILE: Util.java
import org.jspecify.annotations.*;

@NullMarked
public final class Util {
    public static Box<String> makeString() {
        return new Box<>();
    }
}

// FILE: test.kt
fun test() {
    Util.makeString().consume(null)
}
