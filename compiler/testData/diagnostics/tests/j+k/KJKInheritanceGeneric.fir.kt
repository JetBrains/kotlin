// FILE: K1.kt
open class KFirst<T: java.io.Serializable>() {
    fun foo(t: T): T = t
}

// FILE: J1.java
public class J1 extends KFirst<Integer> {
    void baz() {}
}

// FILE: K2.kt
class K2: J1() {
    fun bar() {
        foo(1)
        baz()
    }
}