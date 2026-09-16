// RUN_PIPELINE_TILL: CODEGEN
private enum class Foo { A, B }

private class Bar(val foo: Foo)

/* GENERATED_FIR_TAGS: classDeclaration, enumDeclaration, enumEntry, primaryConstructor, propertyDeclaration */
