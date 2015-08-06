// !DIAGNOSTICS: -UNUSED_VARIABLE
// FILE: A.java

import java.util.*;

class A<T> {
    List<T> x;

    void foo(T x, List<T> y) {}

    A<List<T>> bar() {}
}

// FILE: Test.java

class Test {
    static class RawADerived extends A {

    }
}

// FILE: main.kt

val strList: List<String> = null!!
val strMap: Map<String, String> = null!!

fun main() {
    val rawADerived = Test.RawADerived()
    rawADerived.x = strList
    rawADerived.foo("", strList)


    val rawA = rawADerived.bar()
    rawA.x = strList
    rawA.foo("", strList)
}
