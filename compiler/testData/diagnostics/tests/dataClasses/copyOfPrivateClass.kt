// RUN_PIPELINE_TILL: CODEGEN
class Outer {
    private data class Nested(val c: Int)
}

/* GENERATED_FIR_TAGS: classDeclaration, data, nestedClass, primaryConstructor, propertyDeclaration */
