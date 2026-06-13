// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-30352

// KT-30352: Lambda type inference should not be necessary when only a single overload accepts a lambda

class A<T>

class B {
    fun <T> foo(a: A<T>, value: T) {}
    fun <T> foo(a: A<T>, value: (A<T>) -> T) {}
}

fun usage(b: B) {
    b.foo(A(), 10)
    b.foo(A<Long>()) { 10 }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, integerLiteral, lambdaLiteral,
nullableType, typeParameter */
