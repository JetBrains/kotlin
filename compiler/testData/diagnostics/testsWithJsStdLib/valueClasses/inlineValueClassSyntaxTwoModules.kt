// RUN_PIPELINE_TILL: FRONTEND

// MODULE: lib
inline value class A(val x: Int)
value inline class B(val x: String)

// MODULE: main(lib)
inline value class C(val a: A)
value inline class D(val b: B)

fun f(<!FORBIDDEN_VARARG_PARAMETER_TYPE!>vararg<!> xs: A) {}
fun g(<!FORBIDDEN_VARARG_PARAMETER_TYPE!>vararg<!> xs: B) {}
fun h(<!FORBIDDEN_VARARG_PARAMETER_TYPE!>vararg<!> xs: C) {}
fun i(<!FORBIDDEN_VARARG_PARAMETER_TYPE!>vararg<!> xs: D) {}
