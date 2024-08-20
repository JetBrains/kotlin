// FIR_IDENTICAL
// ISSUES: KT-69985, KT-69986
// FILE: file1.kt
open class ClassFromRoot {
    companion object {
        fun foo() {}
    }

    class Nested
}

object ObjFromRoot {
    fun foo() {}
}

fun funFromRoot() {}

val valFromRoot = 1

// FILE: J.java
public class J {
    public static String x = "O";
    public static String h() { return "O"; }
    public static class B {}
}

// FILE: file2.kt
package test

class X : <!UNRESOLVED_REFERENCE!>ClassFromRoot<!>()
class Y : <!UNRESOLVED_REFERENCE!>J<!>()
class Z: J.B()

fun usage() {
    val foo: <!UNRESOLVED_REFERENCE!>ClassFromRoot<!> = <!UNRESOLVED_REFERENCE!>ClassFromRoot<!>()
    val foo2: ClassFromRoot.Companion = ClassFromRoot.Companion
    val foo3: ClassFromRoot.Nested = ClassFromRoot.Nested()

    <!UNRESOLVED_REFERENCE!>funFromRoot<!>()
    <!UNRESOLVED_REFERENCE!>valFromRoot<!>

    ClassFromRoot.foo()
    ClassFromRoot.Companion.foo()

    ObjFromRoot.foo()

    val bar: <!UNRESOLVED_REFERENCE!>J<!> = <!UNRESOLVED_REFERENCE!>J<!>()
    val baz: J.B = J.B()
    J.x
    J.h()
}