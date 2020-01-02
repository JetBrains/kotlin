// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER

interface MemoizedFunctionToNotNull<K, V>

fun <K, V : Any> createMemoizedFunction(compute: (K) -> V): MemoizedFunctionToNotNull<K, V> = TODO()

interface A

interface TypeConstructor

class Refiner {
    val memoizedFunctionLambda = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>createMemoizedFunction<!> { <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>it<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>foo<!>() } // error type infered, no diagnostic, BAD, backend fails
    val memoizedFunctionReference = createMemoizedFunction(TypeConstructor::<!EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED!>foo<!>) // EXTENSION_IN_CLASS_REFERENCE_IS_NOT_ALLOWED, fine
    val memoizedFunctionTypes = createMemoizedFunction<TypeConstructor, Boolean> { it.foo() } // works fine

    private fun TypeConstructor.foo(): Boolean = true
}
