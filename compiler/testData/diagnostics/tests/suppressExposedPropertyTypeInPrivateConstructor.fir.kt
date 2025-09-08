// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-57458

private enum class Foo { A, B }

class Bar private constructor(
    @Suppress(<!ERROR_SUPPRESSION!>"EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR_ERROR"<!>)
    val foo: Foo,
)

class Var private constructor(
    @property:Suppress(<!ERROR_SUPPRESSION!>"EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR_ERROR"<!>)
    val <!EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR_ERROR!>foo<!>: Foo,
)

class Zar private constructor(
    @param:Suppress(<!ERROR_SUPPRESSION!>"EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR_ERROR"<!>)
    val foo: Foo,
)

/* GENERATED_FIR_TAGS: annotationUseSiteTargetParam, annotationUseSiteTargetProperty, classDeclaration, enumDeclaration,
enumEntry, primaryConstructor, propertyDeclaration, stringLiteral */
