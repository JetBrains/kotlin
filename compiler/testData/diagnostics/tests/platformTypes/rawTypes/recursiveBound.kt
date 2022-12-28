// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE
// FILE: A.java

import java.util.List;

public interface A<T extends A> {}
// FILE: main.kt

class D : A<D>

fun main(x: A<*>) {
    if (x is D) {

    }
}