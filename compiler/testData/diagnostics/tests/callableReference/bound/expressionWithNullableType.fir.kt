// FILE: J.java

public interface J {
    String platformString();
}

// FILE: test.kt

fun f1(x: Int?): Any = x::hashCode
fun <T> f2(t: T): Any = t::hashCode
fun <S : String?> f3(s: S): Any = s::hashCode
fun <U : Any> f4(u: U?): Any = u::hashCode
fun f5(c: List<*>): Any = c[0]::hashCode

fun f6(j: J): Any = j.platformString()::hashCode
