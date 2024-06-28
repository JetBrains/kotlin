// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FULL_JDK
// SEPARATE_SIGNATURE_DUMP_FOR_K2
// ^ ISSUE: KT-65219, KT-63914

// FILE: Java1.java
public class Java1 extends KotlinClass { }

// FILE: 1.kt
import java.util.ArrayList

class A : Java1()

open class KotlinClass : ArrayList<Int>()

fun test(a: A) {
    a.size
    a.add(1)
    a.add(1,2)
    a.get(0)
    a.removeAt(1)
    a.remove(1)
}