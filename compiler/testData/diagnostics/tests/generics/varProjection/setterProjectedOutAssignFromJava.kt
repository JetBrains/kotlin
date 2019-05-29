// !WITH_NEW_INFERENCE
// Issue: KT-31594

// FILE: Tr.java

public class Tr<T> {
    public T getV() { return null; }
    public void setV(T value) {}
}

// FILE: main.kt

fun test(t: Tr<*>) {
    <!NI;SETTER_PROJECTED_OUT!>t.v<!> = null
    <!NI;SETTER_PROJECTED_OUT!>t.v<!> = <!NI;TYPE_MISMATCH!>""<!>
}