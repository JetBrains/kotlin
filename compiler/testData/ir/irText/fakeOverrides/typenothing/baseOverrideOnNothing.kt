// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: Java1.java
public interface Java1 extends A {  }


// FILE: 1.kt
interface A {
    val a : Nothing
    val b: List<Nothing>
    fun foo(a: Nothing)
    fun bar(): Nothing

    val c: Nothing?
    val d: List<Nothing?>
    fun foo2(a: Nothing?)
    fun bar2(): Nothing?
}

interface B : Java1

abstract class C(
    override val a: Nothing,
    override val b: List<Nothing>
) : Java1 {
    override fun bar2(): Nothing? {
        return null
    }

    override fun foo(a: Nothing) {}

}

fun test(b: B, c: C) {
    val k: Nothing = b.a
    b.foo(k)
    val k2: Nothing = b.bar()
    val k3: List<Nothing> = b.b

    val k4: Nothing? = b.c
    b.foo2(k4)
    val k5: Nothing? = b.bar2()
    val k6: List<Nothing?> = b.d


    val k7: Nothing = c.a
    c.foo(k)
    val k8: Nothing = c.bar()
    val k9: List<Nothing> = c.b

    val k10: Nothing? = c.c
    c.foo2(k4)
    val k11: Nothing? = c.bar2()
    val k12: List<Nothing?> = c.d
}