// FILE: J.java

public interface J {
    String platformString();
}

// FILE: test.kt

fun f1(x: Int?): Any = x::<!UNRESOLVED_REFERENCE!>hashCode<!>
fun <T> f2(t: T): Any = t::<!UNRESOLVED_REFERENCE!>hashCode<!>
fun <S : String?> f3(s: S): Any = s::<!UNRESOLVED_REFERENCE!>hashCode<!>
fun <U : Any> f4(u: U?): Any = u::<!UNRESOLVED_REFERENCE!>hashCode<!>
fun f5(c: List<*>): Any = c[0]::<!UNRESOLVED_REFERENCE!>hashCode<!>

fun f6(j: J): Any = j.platformString()::hashCode
