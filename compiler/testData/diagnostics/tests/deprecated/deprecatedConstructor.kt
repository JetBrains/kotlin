// !DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: A.kt
class A(s: String) {
    @Deprecated("")
    constructor(i: Int) : this(i.toString()) {

    }
}

// FILE: B.java
public class B extends A {
    @Deprecated
    public B(int i) {

    }

    public B(String s) {

    }
}

// FILE: C.kt
class C @Deprecated("") constructor(s: String) {
}

// FILE: use.kt
fun use(a: A, b: B, c: C) {
    <!DEPRECATION!>A<!>(3)
    A("")
    <!DEPRECATION!>B<!>(3)
    B("")
    <!DEPRECATION!>C<!>("s")
}
