// FIR_IDENTICAL

// FILE: Super.java
import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class Super<E extends Super<E>> implements Comparable<E> {
    public final int compareTo(E o) {
       throw new RuntimeException();
    }
}

// FILE: test.kt
open class E : Super<E>()