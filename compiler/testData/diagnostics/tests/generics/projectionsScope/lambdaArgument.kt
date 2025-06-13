// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
// CHECK_TYPE

class A<T> {
    fun foo(f: (T) -> Unit) {}
}

fun test(a: A<out Number>, b: A<in Number>) {
    a.foo {
        it checkType { _<Number>() }
    }

    b.foo {
        it checkType { _<Any?>() }
    }
}

/* GENERATED_FIR_TAGS: capturedType, classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType,
inProjection, infix, lambdaLiteral, nullableType, outProjection, typeParameter, typeWithExtension */
