// FILE: J.java

public interface J {
    String platformString();
}

// FILE: test.kt

fun f1(x: Int?): Any = x::class
fun <T> f2(t: T): Any = t::class
fun <S : String?> f3(s: S): Any = s::class
fun <U : Any> f4(u: U?): Any = u::class
fun f5(c: List<*>): Any = c[0]::class

fun f6(j: J): Any = j.platformString()::class
