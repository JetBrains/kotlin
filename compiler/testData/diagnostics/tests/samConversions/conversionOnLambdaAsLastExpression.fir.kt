// FILE: C.java

public interface C { void on(String s); }

// FILE: A.java

public class A { void add(C c) {} }

// FILE: test.kt

class B : A() {
    fun test(x: Any?) {
        add(foo { <!ARGUMENT_TYPE_MISMATCH!>{ _ : String -> Unit }<!> })
        add(x?.let { <!ARGUMENT_TYPE_MISMATCH!>{ _ : String -> Unit }<!> })
    }
}

fun <T> foo(f: () -> T): T = f()
