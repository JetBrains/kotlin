// RUN_PIPELINE_TILL: FRONTEND

inline value class A(val x: Int)
value inline class B(val x: String)

fun f(<!FORBIDDEN_VARARG_PARAMETER_TYPE!>vararg<!> xs: A) {}
fun g(<!FORBIDDEN_VARARG_PARAMETER_TYPE!>vararg<!> xs: B) {}
