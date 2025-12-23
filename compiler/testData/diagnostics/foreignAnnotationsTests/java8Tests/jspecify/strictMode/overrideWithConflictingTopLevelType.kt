// JSPECIFY_STATE: strict
// ISSUE: KT-83314

// FILE: ParentA.java

import org.jspecify.annotations.*;

@NullMarked
public class ParentA {
    public String testNullability(String input) {
        return input;
    }

    @Override
    public boolean equals(Object obj) { // -> actual `equals(@NotNull Object)` ?
        return super.equals(obj);
    }
}

// FILE: main.kt
class SubA : ParentA() {
    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }
}

fun main() {
    SubA().equals(null)
    <!SENSELESS_COMPARISON!>SubA() == null<!>
}