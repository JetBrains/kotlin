// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-88132

typealias Anything = Any

abstract class MyList<T>: List<T & Anything> {
    abstract fun items(): List<T & Anything>
}

/* GENERATED_FIR_TAGS: classDeclaration, dnnType, functionDeclaration, nullableType, typeAliasDeclaration, typeParameter */
