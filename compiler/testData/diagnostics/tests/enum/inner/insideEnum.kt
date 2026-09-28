// RUN_PIPELINE_TILL: CODEGEN
enum class E {
    ABC;
    
    enum class F {
        DEF
    }
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, nestedClass */
