// FILE: J.java
public class J {
    public static void foo() {}
}

// FILE: test.kt
open class A {
    companion object : J() {
        fun bar() {}
    }
}

class B : A() {
    init {
        foo()
        bar()
    }

    fun test2() {
        foo()
        bar()
    }

    object O {
        fun test() {
            foo()
            bar()
        }
    }

    companion object {
        init {
            foo()
            bar()
        }

        fun test() {
            foo()
            bar()
        }

        fun bar() {}
    }
}

