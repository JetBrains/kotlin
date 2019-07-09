// !DIAGNOSTICS: -UNUSED_VARIABLE
// FILE: A.java

import java.util.*;

class A<T> {
    B b;
}

// FILE: B.java

import java.util.*;

class B {
    void bar(List<Double> x);
}

// FILE: Test.java

class Test {
    static class RawADerived extends A {}
    static A rawAField = null;
}


// FILE: main.kt

val strList: List<String> = null!!

fun main() {
    val rawB = Test.rawAField.b;
    // Raw(A).b is not erased because it have no type parameters
    var rawInner = rawB.bar(<!TYPE_MISMATCH("(Mutable)List<Double!>!", "List<String>")!>strList<!>)
}
