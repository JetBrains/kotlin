// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-57458

private enum class Foo { A, B }

class Bar constructor(
    <!EXPOSED_PARAMETER_TYPE!>@Suppress(<!ERROR_SUPPRESSION!>"EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR_ERROR"<!>)
    val foo: Foo<!>,
)

class Var constructor(
    <!EXPOSED_PARAMETER_TYPE!>@property:Suppress(<!ERROR_SUPPRESSION!>"EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR_ERROR"<!>)
    val foo: Foo<!>,
)

class Zar constructor(
    <!EXPOSED_PARAMETER_TYPE!>@param:Suppress(<!ERROR_SUPPRESSION!>"EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR_ERROR"<!>)
    val foo: Foo<!>,
)

/* GENERATED_FIR_TAGS: annotationUseSiteTargetParam, annotationUseSiteTargetProperty, classDeclaration, enumDeclaration,
enumEntry, primaryConstructor, propertyDeclaration, stringLiteral */
