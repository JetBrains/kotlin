// FIR_IDENTICAL
// TARGET_BACKEND: JVM
// FILE: JFieldOwner.java

public class JFieldOwner {
    public int f;
}

// FILE: jvmFieldWithIntersectionTypes.kt

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
