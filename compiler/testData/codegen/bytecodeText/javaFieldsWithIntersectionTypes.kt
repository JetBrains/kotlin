// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR
// ^ See javaFieldsWithIntersectionTypes_k1.kt for a copy of this test for K1.

// FILE: JFieldOwner.java

public class JFieldOwner {
    public int f;
}

// FILE: test.kt

interface IFoo

class Derived1 : JFieldOwner(), IFoo
class Derived2 : JFieldOwner(), IFoo

open class Mid : JFieldOwner()
class DerivedThroughMid1 : Mid(), IFoo
class DerivedThroughMid2 : Mid(), IFoo

fun test(b : Boolean) {
    val d1 = Derived1()
    val d2 = Derived2()
    val k = if (b) d1 else d2
    k.f = 42
    k.f

    val md1 = DerivedThroughMid1()
    val md2 = DerivedThroughMid2()
    val mk = if (b) md1 else md2
    mk.f = 44
    mk.f

}

// K2 generates access to Java1.f in both cases. The main motivation for this is to fix cases like KT-49507.

// 2 GETFIELD JFieldOwner.f : I
// 2 PUTFIELD JFieldOwner.f : I
// 0 GETFIELD Mid.f : I
// 0 PUTFIELD Mid.f : I
