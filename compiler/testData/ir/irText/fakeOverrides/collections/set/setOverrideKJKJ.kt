// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// SEPARATE_SIGNATURE_DUMP_FOR_K2
// ^ ISSUE: KT-65219, KT-63914

// FILE: Java1.java
public class Java1 extends KotlinClass { }

// FILE: 1.kt
import java.util.HashSet

abstract class A : Java1()  //Kotlin ← Java ← Kotlin ← Java

class B : Java1() {
    override val size: Int
        get() = 5

    override fun add(element: String): Boolean {
        return true
    }
}

open class KotlinClass : HashSet<String>()

fun test(a: A, b: B) {
    a.size
    a.add("")
    a.remove<String?>(null)
    a.remove("")

    b.size
    b.add("")
}