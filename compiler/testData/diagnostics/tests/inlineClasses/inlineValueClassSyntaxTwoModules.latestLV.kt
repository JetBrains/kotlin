// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: FRONTEND

// MODULE: lib
inline value class A(val x: Int)
value inline class B(val x: String)

// MODULE: main(lib)
inline value class C(val a: A)
value inline class D(val b: B)

fun f(vararg xs: A) {}
fun g(vararg xs: B) {}
fun h(vararg xs: C) {}
fun i(vararg xs: D) {}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, primaryConstructor, propertyDeclaration, value, vararg */
