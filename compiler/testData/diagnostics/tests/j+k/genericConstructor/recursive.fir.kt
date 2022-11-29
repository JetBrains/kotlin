// FILE: C.java

// See KT-10410
public class C {
    public <T extends T> C(T t) {
    }
}

// FILE: main.kt

fun foo() = <!NO_VALUE_FOR_PARAMETER!>C()<!>
