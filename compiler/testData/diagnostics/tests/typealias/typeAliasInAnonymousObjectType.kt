// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
open class Foo<T>

typealias FooStr = Foo<String>

val test = object : FooStr() {}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, nullableType, propertyDeclaration,
typeAliasDeclaration, typeParameter */
