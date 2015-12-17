// FILE: J.java
public class J {
    public static void foo() {}
}

// FILE: test.kt
class A {
    init {
        foo()
        bar()
    }

    fun test1() {
        foo()
        bar()
    }

    object O {
        fun test() {
            foo()
            bar()
        }
    }

    companion object : J() {
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
