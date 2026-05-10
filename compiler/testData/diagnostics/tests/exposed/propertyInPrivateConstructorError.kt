// RUN_PIPELINE_TILL: FRONTEND

private enum class Foo { A, B }

class Bar private constructor(val <!EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR_ERROR!>foo<!>: Foo)

/* GENERATED_FIR_TAGS: classDeclaration, enumDeclaration, enumEntry, primaryConstructor, propertyDeclaration */
