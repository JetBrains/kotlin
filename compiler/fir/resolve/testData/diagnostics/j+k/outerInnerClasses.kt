// FILE: K1.kt
open class KotlinOuter {
    inner open class KotlinInner {
        fun foo() {}
    }

    fun bar() {}
}

// FILE: J1.java
public class J1 extends KotlinOuter {
    public class J2 extends KotlinInner {
        public void bazbaz() {}
    }

    public void baz() {}
}

// FILE: K2.kt
class K2: J1() {
    fun main() {
        bar()
        baz()
    }

    inner class K3 : J2() {
        fun main() {
            foo()
            bazbaz()
            bar()
            baz()
        }
    }
}
