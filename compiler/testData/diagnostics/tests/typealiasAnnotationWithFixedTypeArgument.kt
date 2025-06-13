// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-68996

annotation class MyAnnotation<T>

typealias FixedAnnotation = MyAnnotation<Int>

class Foo(@FixedAnnotation val inner: Int)

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, nullableType, primaryConstructor, propertyDeclaration,
typeAliasDeclaration, typeParameter */
