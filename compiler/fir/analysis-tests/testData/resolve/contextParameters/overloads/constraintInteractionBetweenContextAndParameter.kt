// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +ContextParameters

interface I<X, Y>

context(c: I<T1, T2>)
<!CONFLICTING_OVERLOADS!>fun <T1, T2> foo(left: T2, right: T1)<!> {}

@JvmName("foo2")
context(c: I<T3, T4>)
<!CONFLICTING_OVERLOADS!>fun <T3, T4> foo(left: T3, right: T4)<!> {}

/* GENERATED_FIR_TAGS: functionDeclaration, functionDeclarationWithContext, interfaceDeclaration, nullableType,
typeParameter */
