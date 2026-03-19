// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-24868
// WITH_STDLIB

// KT-24868: Spurious "Restricted suspending functions can only invoke member or extension suspending functions on their restricted coroutine scope"

object Foo {
    val f: suspend SequenceScope<Int>.(low: Int, high: Int) -> Unit = { low, high ->
        (low until high).forEach {
            yield(it)
        }
    }
}

fun test1() = sequence<Int> {
    Foo.f(this, 1, 10)   // spurious error: should be equivalent to this.f(1, 10)
}

fun test2() = sequence<Int> {
    val f = Foo.f
    this.f(1, 10)        // OK
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, lambdaLiteral, localProperty,
objectDeclaration, propertyDeclaration, suspend, thisExpression, typeWithExtension */
