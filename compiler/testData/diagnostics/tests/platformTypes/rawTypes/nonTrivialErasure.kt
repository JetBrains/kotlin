// !DIAGNOSTICS: -UNUSED_VARIABLE
// FILE: A.java

import java.util.*;

class A<T extends A<T, E>, E extends T, F extends List<? extends Double>> {
    T first;
    E second;
    F listOfDoubles;
}

// FILE: Test.java

class Test {
    static class DerivedRawA extends A {}
    static A rawAField = null;
}

// FILE: main.kt
val strList: List<String> = null!!

fun main() {
    val rawA = Test.rawAField
    rawA.first = <!TYPE_MISMATCH!>Test.rawAField.second<!>
    Test.rawAField.second = <!TYPE_MISMATCH!>rawA.first.second<!>

    rawA.listOfDoubles = <!TYPE_MISMATCH!>strList<!>
    rawA.listOfDoubles = <!TYPE_MISMATCH!>""<!> // first should be List
}
