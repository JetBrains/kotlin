// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-88132

typealias Anything = Any

abstract class MyList<T>: List<T & <!INCORRECT_RIGHT_COMPONENT_OF_INTERSECTION!>Anything<!>> {
    abstract fun items(): List<T & Anything>
}

/* GENERATED_FIR_TAGS: classDeclaration, dnnType, functionDeclaration, nullableType, typeAliasDeclaration, typeParameter */
