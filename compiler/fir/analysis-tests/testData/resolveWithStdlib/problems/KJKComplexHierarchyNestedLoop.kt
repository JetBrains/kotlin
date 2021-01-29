// FILE: K1.kt
class K2: J1() {
    class Q : <!UNRESOLVED_REFERENCE!>Nested<!>()
    fun bar() {
        <!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>foo<!>()<!>
        <!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>baz<!>()<!>

        <!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>superClass<!>()<!>
        <!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>superI<!>()<!>
    }
}

// FILE: J1.java
public class J1 extends K2() {
    public class Nested {}

    void baz() {}
}

// FILE: K2.kt
open class KFirst : SuperClass(), SuperI {
    fun foo() {
    }
}

// FILE: K3.kt
abstract class SuperClass {
    fun superClass() {}
}

interface SuperI {
    fun superI() {}
}
