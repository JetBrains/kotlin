// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// SEPARATE_SIGNATURE_DUMP_FOR_K2
// ^ ISSUE: KT-63914

// FILE: Java1.java
public class Java1<T> extends KotlinClass<T> { }

// FILE: 1.kt

import java.util.ArrayList

class A<T> : Java1<T>()

open class KotlinClass<T> : ArrayList<T>()

fun test(a: A<Int>) {
    a.size
    a.add(1)
    a.add(1,2)
    a.get(0)
    a.removeAt(1)
    a.remove(1)
}