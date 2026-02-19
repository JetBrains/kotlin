// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

fun test() {
    generateException(Data(1), { Data(it.x + 2) })
}

fun <T> generateException(a: T, next: (T) -> T) {}

class Data<out K>(val x: K)

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, functionDeclaration, functionalType, integerLiteral,
lambdaLiteral, nullableType, out, primaryConstructor, propertyDeclaration, typeParameter */
