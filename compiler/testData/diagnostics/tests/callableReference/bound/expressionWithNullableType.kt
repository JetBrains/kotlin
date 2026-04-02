// RUN_PIPELINE_TILL: FRONTEND
// FILE: J.java

public interface J {
    String platformString();
}

// FILE: test.kt

fun f1(x: Int?): Any = x::<!UNSAFE_CALLABLE_REFERENCE!>hashCode<!>
fun <T> f2(t: T): Any = t::<!UNSAFE_CALLABLE_REFERENCE!>hashCode<!>
fun <S : String?> f3(s: S): Any = s::<!UNSAFE_CALLABLE_REFERENCE!>hashCode<!>
fun <U : Any> f4(u: U?): Any = u::<!UNSAFE_CALLABLE_REFERENCE!>hashCode<!>
fun f5(c: List<*>): Any = c[0]::<!UNSAFE_CALLABLE_REFERENCE!>hashCode<!>

fun f6(j: J): Any = j.platformString()::hashCode

/* GENERATED_FIR_TAGS: callableReference, flexibleType, functionDeclaration, integerLiteral, javaFunction, javaType,
nullableType, starProjection, typeConstraint, typeParameter */
