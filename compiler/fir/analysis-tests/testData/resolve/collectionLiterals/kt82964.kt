// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82964

@Repeatable
annotation class Foo<T>(val arr: Array<Bar<T>>)
annotation class Bar<T>

@Foo<Int>(<!ARGUMENT_TYPE_MISMATCH!>[Bar()]<!>)
@Foo<Int>([Bar<Int>()])
fun test() {
}

/* GENERATED_FIR_TAGS: annotationDeclaration, collectionLiteral, functionDeclaration, nullableType, primaryConstructor,
propertyDeclaration, typeParameter */
