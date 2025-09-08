// RUN_PIPELINE_TILL: BACKEND
class A
val <X> X.prop: Int get() = 1
fun <X> X.baz(): Int = 1

fun foo(x: (A) -> Int) {}

fun main() {
    foo(A::prop)
    foo(A::baz)
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionalType, getter, integerLiteral, nullableType, propertyDeclaration, propertyWithExtensionReceiver, typeParameter */
