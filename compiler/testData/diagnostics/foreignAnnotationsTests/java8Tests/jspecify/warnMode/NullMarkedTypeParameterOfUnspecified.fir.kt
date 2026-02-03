// JSPECIFY_STATE: warn
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
    Util.makeString().consume(<!TYPE_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
}
