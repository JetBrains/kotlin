// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-46675
// WITH_STDLIB

// KT-46675: Cryptic "Nothing was expected" compiler error with new type inference
interface I<T>
fun <T> foo(bar: Iterable<I<in T>>): I<T> = TODO()
fun <T, U : T> I<T>.baz(that: I<U>) = foo<U>(<!ARGUMENT_TYPE_MISMATCH("List<I<out T (of fun <T, U : T> I<T>.baz)>>; Iterable<I<in U (of fun <T, U : T> I<T>.baz)>>")!>listOf(this, that)<!>)

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, inProjection, interfaceDeclaration, nullableType,
outProjection, thisExpression, typeConstraint, typeParameter */
