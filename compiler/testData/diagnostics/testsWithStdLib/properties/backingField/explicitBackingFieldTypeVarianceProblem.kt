// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-83590

class In<in T>(x: T) {
    val a: Any?
        field = x
    fun other(x: In<String>) = x.a
}

fun main() {
    val v: In<String> = In(Any())
    val x: String = In(1).<!INITIALIZER_TYPE_MISMATCH!>other<!>(v)
}

/* GENERATED_FIR_TAGS: classDeclaration, explicitBackingField, functionDeclaration, in, integerLiteral, localProperty,
nullableType, primaryConstructor, propertyDeclaration, smartcast, typeParameter */
