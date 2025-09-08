// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_EXPRESSION

class A {
    fun foo(i: A) {}

    fun baz(i: A) {}
}

class B {
    fun foo(s: B) {}
    fun foo(c: Char) {}

    fun baz(s: B) {}
}

fun <T> bar(f: (T) -> Unit): T = TODO()

fun test() {
    <!CANNOT_INFER_PARAMETER_TYPE!>myWith<!>(A()) {
        val t1 = bar(::foo)
        t1

        val t2 = bar(::baz)
        t2

        <!CANNOT_INFER_PARAMETER_TYPE!>myWith<!>(B()) {
            val a: A = bar(::foo)
            val b: B = bar(::foo)

            val t3 = bar(::baz)
            t3

            <!CANNOT_INFER_PARAMETER_TYPE!>bar<!>(::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>)
        }
    }
}

inline fun <T, R> myWith(receiver: T, block: T.() -> R): R = TODO()

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, functionalType, inline, lambdaLiteral,
localProperty, nullableType, propertyDeclaration, typeParameter, typeWithExtension */
