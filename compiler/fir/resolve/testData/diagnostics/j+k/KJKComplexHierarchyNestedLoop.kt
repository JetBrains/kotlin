// FILE: K1.kt
class K2: J1() {
    class Q : Nested()
    fun bar() {
        foo()
        baz()

        superClass()
        superI()
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
