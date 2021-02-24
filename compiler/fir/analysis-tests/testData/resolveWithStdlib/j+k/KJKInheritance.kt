// FILE: K1.kt
open class KFirst() {
    fun foo() {
    }
}

// FILE: J1.java
public class J1 extends KFirst {
    void baz() {}
}

// FILE: K2.kt
class K2: J1() {
    fun bar() {
        foo()
        baz()
    }
}