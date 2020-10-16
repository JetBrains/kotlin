// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER

interface MemoizedFunctionToNotNull<K, V>

fun <K, V : Any> createMemoizedFunction(compute: (K) -> V): MemoizedFunctionToNotNull<K, V> = TODO()

interface A

interface TypeConstructor

class Refiner {
    val memoizedFunctionLambda = createMemoizedFunction { it.foo() } // error type infered, no diagnostic, BAD, backend fails
    val memoizedFunctionReference = <!INAPPLICABLE_CANDIDATE!>createMemoizedFunction<!>(<!UNRESOLVED_REFERENCE!>TypeConstructor::foo<!>) // EXTENSION_IN_CLASS_REFERENCE_IS_NOT_ALLOWED, fine
    val memoizedFunctionTypes = createMemoizedFunction<TypeConstructor, Boolean> { it.foo() } // works fine

    private fun TypeConstructor.foo(): Boolean = true
}
