// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// MODULE: common
expect class Typealiased

annotation class Ann(val p: kotlin.reflect.KClass<*>)

@Ann(Typealiased::class)
expect fun test()

@Ann(<!CLASS_LITERAL_LHS_NOT_A_CLASS!>Array<Typealiased>::class<!>)
expect fun testInArray()

// MODULE: main()()(common)
class TypealiasedImpl

actual typealias Typealiased = TypealiasedImpl

@Ann(Typealiased::class)
actual fun test() {}

@Ann(Array<Typealiased>::class)
actual fun testInArray() {}

/* GENERATED_FIR_TAGS: actual, annotationDeclaration, classDeclaration, classReference, expect, functionDeclaration,
primaryConstructor, propertyDeclaration, starProjection, typeAliasDeclaration */
