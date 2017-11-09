// FILE: J.java

public interface J {
    String platformString();
}

// FILE: test.kt

fun f1(x: Int?): Any = x::<!UNSAFE_CALL!>hashCode<!>
fun <T> f2(t: T): Any = t::<!UNSAFE_CALL!>hashCode<!>
fun <S : String?> f3(s: S): Any = s::<!UNSAFE_CALL!>hashCode<!>
fun <U : Any> f4(u: U?): Any = u::<!UNSAFE_CALL!>hashCode<!>
fun f5(c: List<*>): Any = c[0]::<!UNSAFE_CALL!>hashCode<!>

fun f6(j: J): Any = j.platformString()::hashCode
