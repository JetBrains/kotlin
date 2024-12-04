// FIR_IDENTICAL
// MODULE: library
// FILE: test/A.java
package test;

import java.util.*;

public class A<T> {
    A<List<T>> bar(List<Map<String, Integer>> x) { return null; }

    static A rawField = null;
}

// FILE: test/lib.kt
package test

val strList: List<String> = null!!

fun foo1() = A.rawField

val foo2 = A.rawField.bar(strList).bar(strList)

// MODULE: main(library)
// FILE: main.kt
package test

fun foo3() = foo1().bar(strList)
val foo4 = foo2.bar(strList)
