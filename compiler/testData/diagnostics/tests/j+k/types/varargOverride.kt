// SKIP_TXT
// FIR_IDENTICAL
// FILE: A.java

import org.jetbrains.annotations.NotNull;

public interface A extends B {
    void foo(@NotNull String... value);
}

// FILE: main.kt
interface B {
    fun foo(vararg x: String) {}
}

fun main(a: A) {
    a.foo()
    a.foo("1")
    a.foo("2", "3")
}
