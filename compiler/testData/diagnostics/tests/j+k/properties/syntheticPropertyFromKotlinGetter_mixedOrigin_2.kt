// FIR_IDENTICAL
// ISSUE: KT-63076

// FILE: AK.kt
open class AK {
    open fun getX1(): String = ""
    open fun getX2(): String = ""
    open fun getX3(): String = ""
    open fun getX4(): String = ""
}

// FILE: AJ.java
public interface AJ {
    String getX1();
    String getX2();
    String getX3();
    String getX4();
}

// FILE: B.kt
open class B : AK(), AJ {
    override fun getX2(): String = ""
    override fun getX3(): String = ""
    override fun getX4(): String = ""
}

// FILE: C.java
public class C extends B {
    public String getX3() { return ""; }
    public String getX4() { return ""; }
}

// FILE: D.kt
open class D : C() {
    override fun getX4(): String = ""
}

// FILE: main.kt

fun test_1(b: B) {
    b.x1
    b.x2
    b.x3
    b.x4
}

fun test_2(c: C) {
    c.x1
    c.x2
    c.x3
    c.x4
}

fun test_3(d: D) {
    d.x1
    d.x2
    d.x3
    d.x4
}
