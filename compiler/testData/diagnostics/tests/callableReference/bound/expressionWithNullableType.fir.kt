// FILE: J.java

public interface J {
    String platformString();
}

// FILE: test.kt

fun f1(x: Int?): Any = <!UNRESOLVED_REFERENCE!>x::hashCode<!>
fun <T> f2(t: T): Any = <!UNRESOLVED_REFERENCE!>t::hashCode<!>
fun <S : String?> f3(s: S): Any = <!UNRESOLVED_REFERENCE!>s::hashCode<!>
fun <U : Any> f4(u: U?): Any = <!UNRESOLVED_REFERENCE!>u::hashCode<!>
fun f5(c: List<*>): Any = <!UNRESOLVED_REFERENCE!>c[0]::hashCode<!>

fun f6(j: J): Any = j.platformString()::hashCode
