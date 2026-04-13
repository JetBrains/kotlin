// RUN_PIPELINE_TILL: BACKEND
annotation class Base(val x: Int)

annotation class UseBase(val b: Base = Base(0))

@UseBase class My

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, integerLiteral, primaryConstructor, propertyDeclaration */
