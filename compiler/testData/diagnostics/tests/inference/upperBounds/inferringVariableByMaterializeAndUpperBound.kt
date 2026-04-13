// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CAST_NEVER_SUCCEEDS

interface I

interface Inv<P>
interface Out<out T>

class Bar<U : I>(val x: Inv<Out<U>>)

fun <T> materializeFoo(): Inv<T> = null as Inv<T>

fun main() {
    <!CANNOT_INFER_PARAMETER_TYPE!>Bar<!>(<!CANNOT_INFER_PARAMETER_TYPE!>materializeFoo<!>())
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, functionDeclaration, interfaceDeclaration, nullableType, out,
primaryConstructor, propertyDeclaration, typeConstraint, typeParameter */
