// RUN_PIPELINE_TILL: BACKEND
fun add(a: Int, b: Int) = a + b
interface A {
    fun <T> shuffle(x: List<T>): List<T>
    fun <T> foo(f : (List<T>) -> List<T>, x : List<T>)

fun f() : (Int, Int) -> Int = ::add
}

/* GENERATED_FIR_TAGS: additiveExpression, callableReference, functionDeclaration, functionalType, interfaceDeclaration,
nullableType, typeParameter */
