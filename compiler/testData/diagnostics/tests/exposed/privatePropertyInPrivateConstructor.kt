// RUN_PIPELINE_TILL: CODEGEN
private enum class Foo { A, B }

class Bar private constructor(private val foo: Foo)

/* GENERATED_FIR_TAGS: classDeclaration, enumDeclaration, enumEntry, primaryConstructor, propertyDeclaration */
