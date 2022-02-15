// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt

expect class A {
    fun <T : Any> foo(): Unit

    fun <S : Comparable<S>> bar(): List<S>
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual typealias A = JavaA

// FILE: JavaA.java
import java.util.List;

public class JavaA {
    public <T> void foo() {}

    public <S extends Comparable<S>> List<S> bar() {
        return null;
    }
}
