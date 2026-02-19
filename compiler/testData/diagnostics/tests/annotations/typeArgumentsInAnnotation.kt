// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-48444

annotation class Foo<T>(val s: String)

@Foo<Int>("")
fun foo() {
}

/* GENERATED_FIR_TAGS: annotationDeclaration, functionDeclaration, nullableType, primaryConstructor, propertyDeclaration,
stringLiteral, typeParameter */
