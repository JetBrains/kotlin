// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-37975

@Deprecated("")
enum class Foo(val x: Int) {
    A(42)
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, primaryConstructor, propertyDeclaration, stringLiteral */
