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

// FILE: B.java
public class B extends AK implements AJ {
    public String getX2() { return ""; }
    public String getX3() { return ""; }
    public String getX4() { return ""; }
}

// FILE: C.kt
open class C : B() {
    override fun getX3(): String = ""
    override fun getX4(): String = ""
}

// FILE: D.java
public class D extends C {
    public String getX4() { return ""; }
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
