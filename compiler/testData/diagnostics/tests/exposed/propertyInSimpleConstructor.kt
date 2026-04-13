// RUN_PIPELINE_TILL: FRONTEND
private enum class Foo { A, B }

class Bar(<!EXPOSED_PARAMETER_TYPE!>val foo: Foo<!>)

/* GENERATED_FIR_TAGS: classDeclaration, enumDeclaration, enumEntry, primaryConstructor, propertyDeclaration */
