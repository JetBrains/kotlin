// !API_VERSION: 1.0
// FILE: J.java

public interface J {
    void foo();
}

// FILE: test.kt

interface I10 {
    @SinceKotlin("1.0")
    fun foo()
}

interface I11 {
    @SinceKotlin("1.1")
    fun foo()
}

fun f1(x: I10) = x.foo()
fun f2(x: I11) = x.<!UNRESOLVED_REFERENCE!>foo<!>()
fun f3(x: J) = x.foo()

interface BothI1 : I10, I11
fun f4(x: BothI1) = x.foo()

interface BothI2 : I11, I10
fun f5(x: BothI2) = x.foo()

interface JAndI10 : J, I10
fun f6(x: JAndI10) = x.foo()

interface JAndI11 : J, I11
fun f7(x: JAndI11) = x.foo()
