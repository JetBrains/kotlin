// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-85684
// WITH_STDLIB

class Box<T>(val value: T?)
fun foo() = <!CANNOT_INFER_PARAMETER_TYPE!>buildList<!> {
    fun <T> local() {
        add(Box<T>(null))
    }
    local<String>()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, lambdaLiteral, localFunction, nullableType,
primaryConstructor, propertyDeclaration, typeParameter */
