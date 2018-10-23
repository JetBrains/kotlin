
// FILE: A.java

import kotlin.jvm.functions.FunctionN;

public class A {
    public void foo(FunctionN<?> w) { }
}

// FILE: main.kt

class <!UNSUPPORTED(Inheritance of a Java member referencing 'kotlin.jvm.functions.FunctionN': fun foo\(w: FunctionN<*>!\): Unit defined in A)!>B<!> : A()

fun foo() {
    <!UNSUPPORTED(Inheritance of a Java member referencing 'kotlin.jvm.functions.FunctionN': fun foo\(w: FunctionN<*>!\): Unit defined in A)!>object<!> : A() {}
}
