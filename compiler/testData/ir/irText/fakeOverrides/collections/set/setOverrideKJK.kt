// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB

// FILE: Java1.java
import kotlin.collections.AbstractMutableSet;

public abstract class Java1 extends AbstractMutableSet<Object> { }

// FILE: 1.kt
abstract class A : Java1()  //Kotlin ← Java ← Kotlin

class B(override val size: Int) : Java1() {
    override fun add(element: Any?): Boolean {
        return true
    }

    override fun iterator(): MutableIterator<Any> {
        return null!!
    }
}

fun test(a: A, b: B) {
    a.size
    a.add(1)
    a.add(null)
    a.first()
    a.remove(1)
    a.removeAll(listOf(null))
    a.isNotEmpty()

    b.size
    b.add(1)
    b.add(null)
    b.first()
    b.remove(1)
    b.removeAll(listOf(null))
    b.isNotEmpty()
}