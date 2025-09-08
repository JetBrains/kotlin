// RUN_PIPELINE_TILL: FRONTEND
package test

open class ToResolve<SomeClass>(f : (Int) -> Int)
fun testFun(a : Int) = 12

class TestSome<P> {
    companion object : ToResolve<<!UNRESOLVED_REFERENCE!>P<!>>({testFun(it)}) {
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, functionalType, integerLiteral,
lambdaLiteral, nullableType, objectDeclaration, primaryConstructor, typeParameter */
