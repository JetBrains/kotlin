// !DIAGNOSTICS: -UNUSED_VARIABLE
// FILE: A.java
public class A {
    public static int foo() { return 1; }
    public static int bar = 1;
}

// FILE: 1.kt

class B: A() {
    companion object {
        init {
            val a: Int = foo()
            val b: Int = bar
        }
    }

    init {
        val a: Int = foo()
        val b: Int = bar
    }
}

open class C: A() {
    val bar = ""
    fun foo() = ""

    init {
        val a: String = foo()
        val b: String = bar
    }
}

class E: C() {
    init {
        val a: String = foo()
        val b: String = bar
    }
}

open class F: A() {
    companion object {
        val bar = ""
        fun foo() = ""

        init {
            val a: String = foo()
            val b: String = bar
        }
    }
    init {
        val a: String = foo()
        val b: String = bar
    }
}

class G: F() {
    companion object {
        init {
            val a: String = foo()
            val b: String = bar
        }
    }

    init {
        val a: String = foo()
        val b: String = bar
    }
}