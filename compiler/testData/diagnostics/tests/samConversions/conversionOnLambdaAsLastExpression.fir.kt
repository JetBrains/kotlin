// FILE: C.java

public interface C { void on(String s); }

// FILE: A.java

public class A { void add(C c) {} }

// FILE: test.kt

class B : A() {
    fun test(x: Any?) {
        add(foo { <!TYPE_MISMATCH!>{ _ : String -> Unit }<!> })
        add(x?.let { <!TYPE_MISMATCH!>{ _ : String -> Unit }<!> })
    }
}

fun <T> foo(f: () -> T): T = f()
