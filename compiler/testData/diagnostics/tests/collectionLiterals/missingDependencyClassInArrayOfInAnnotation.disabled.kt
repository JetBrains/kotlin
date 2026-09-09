// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -CollectionLiterals
// LANGUAGE_FEATURE_TOGGLED: CollectionLiteralsBasedAnnotationResolution
// WITH_STDLIB
// ISSUE: KT-89259

// MODULE: m1

open class Missing

// MODULE: m2(m1)

annotation class WithMissingInParams(val param: Array<kotlin.reflect.KClass<out Missing>>)

// MODULE: m3(m2)

@WithMissingInParams([])
class Annotated

@WithMissingInParams(arrayOf())
class Annotated2

fun forReference() {
    <!MISSING_DEPENDENCY_CLASS!>WithMissingInParams<!>(<!MISSING_DEPENDENCY_CLASS!>arrayOf<!>())
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, outProjection, primaryConstructor, propertyDeclaration */
