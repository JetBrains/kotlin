// RUN_PIPELINE_TILL: FRONTEND
// WITH_EXTRA_CHECKERS
// ISSUE: KT-77041

@Target(
    AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.TYPEALIAS, AnnotationTarget.FIELD, AnnotationTarget.TYPE,
)
annotation class Anno(val i: Int)

annotation class AnnoWithDefault(val i: Int = fun <!ANONYMOUS_FUNCTION_WITH_NAME!>foo<!>() = 1)

@Anno(i = fun <!ANONYMOUS_FUNCTION_WITH_NAME!>foo<!>() = 1)
fun function(@Anno(i = fun <!ANONYMOUS_FUNCTION_WITH_NAME!>foo<!>() = 1) x: @Anno(i = fun <!ANONYMOUS_FUNCTION_WITH_NAME!>foo<!>() = 1) Int): @Anno(i = fun <!ANONYMOUS_FUNCTION_WITH_NAME!>bar<!>() = 1) Int = x

@Anno(i = fun <!ANONYMOUS_FUNCTION_WITH_NAME!>foo<!>() = 1)
val property: Int = 0

@get:Anno(i = fun <!ANONYMOUS_FUNCTION_WITH_NAME!>foo<!>() = 1)
val propertyWithGetter: Int get() = 0

@Anno(i = fun <!ANONYMOUS_FUNCTION_WITH_NAME!>foo<!>() = 1)
typealias Alias = @Anno(i = fun <!ANONYMOUS_FUNCTION_WITH_NAME!>foo<!>() = 1) Int

class Outer {
    @Anno(i = fun <!ANONYMOUS_FUNCTION_WITH_NAME!>foo<!>() = 1)
    fun member() {}

    @Anno(i = fun <!ANONYMOUS_FUNCTION_WITH_NAME!>foo<!>() = 1)
    val memberProperty: Int = 0
}

enum class E {
    @Anno(i = fun <!ANONYMOUS_FUNCTION_WITH_NAME!>foo<!>() = 1)
    Entry
}

/* GENERATED_FIR_TAGS: annotationDeclaration, annotationUseSiteTargetPropertyGetter, classDeclaration, enumDeclaration,
enumEntry, functionDeclaration, getter, integerLiteral, localFunction, primaryConstructor, propertyDeclaration,
typeAliasDeclaration */
