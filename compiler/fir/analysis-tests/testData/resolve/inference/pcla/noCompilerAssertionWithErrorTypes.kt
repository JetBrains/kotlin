// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// ISSUE: KT-81254

interface MyFlow<out R>

inline fun <X, Y> MyFlow<X>.myFlatMapLatest(crossinline transform: suspend (value: X) -> MyFlow<Y>): MyFlow<Y> = TODO()

inline fun <reified U, V> myCombine(
    vararg flows: MyFlow<U>,
    crossinline transform: suspend (Array<U>) -> V
): MyFlow<V> = TODO()

inline fun <E, F> MyFlow<E>.myMap(crossinline transform: suspend (value: E) -> F): MyFlow<F> = TODO()

val x = <!UNRESOLVED_REFERENCE!>nonExistingFunction<!>()

fun main() {
    x.<!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE!>myFlatMapLatest<!> { <!CANNOT_INFER_PARAMETER_TYPE!>y<!> ->
        // F <: U
        <!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE!>myCombine<!>(y.<!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE!>myMap<!> { <!CANNOT_INFER_PARAMETER_TYPE!>z<!> -> z.<!UNRESOLVED_REFERENCE!>items<!> }) { w /*: Array<U> */ ->
            // U <: T
            // F <: T
            w.flatMap { <!UNRESOLVED_REFERENCE!>u<!> }
        }
    }
}

/* GENERATED_FIR_TAGS: crossinline, funWithExtensionReceiver, functionDeclaration, functionalType, inline,
interfaceDeclaration, lambdaLiteral, nullableType, propertyDeclaration, reified, suspend, typeParameter, vararg */
