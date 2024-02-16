// FIR_IDENTICAL
// FILE: Java1.java
import java.util.ArrayList;

public abstract class Java1 extends ArrayList { }

// FILE: test.kt
abstract class C1 : Java1() {
    override fun removeAll(elements: Collection<*>): Boolean {
        return true
    }
}

fun test(java1: Java1, l: List<*>) {
    java1.removeAll(l)
}