// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// LANGUAGE: +ContextParameters

interface I<X, Y>

context(c: I<T1, T2>)
fun <T1, T2> foo(left: T2, right: T1) {}

@JvmName("foo2")
context(c: I<T3, T4>)
fun <T3, T4> foo(left: T3, right: T4) {}

@JvmName("foo3")
context(c: I<T5, T6>, _: Map<T5, T6>)
<!CONTEXTUAL_OVERLOAD_SHADOWED!>fun <T5, T6> foo(left: T5, right: T6)<!> {}

/* GENERATED_FIR_TAGS: functionDeclaration, functionDeclarationWithContext, interfaceDeclaration, nullableType,
typeParameter */
