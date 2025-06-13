// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
open class Base<A>
class Some: Base<Int>()

// No erased types in check
fun <A> f(a: Base<A>) = a is Some

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, isExpression, nullableType, typeParameter */
