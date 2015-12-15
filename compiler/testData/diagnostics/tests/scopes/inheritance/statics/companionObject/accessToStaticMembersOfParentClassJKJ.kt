// FILE: J.java
public class J {
    public static void foo() {}
}

// FILE: J2.java
public class J2 extends A {
    public static void boo() {}
}

// FILE: test.kt
open class A {
    companion object : J() {
        fun bar() {}
    }
}

class B : J2() {
    init {
        foo()
        bar()
        boo()
    }

    fun test2() {
        foo()
        bar()
        boo()
    }

    object O {
        fun test() {
            foo()
            bar()
            boo()
        }
    }

    companion object {
        init {
            foo()
            bar()
            boo()
        }

        fun test() {
            foo()
            bar()
            boo()
        }

        fun bar() {}
    }
}
