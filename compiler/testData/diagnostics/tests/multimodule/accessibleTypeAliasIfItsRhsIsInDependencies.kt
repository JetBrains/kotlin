// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-79781

// MODULE: base
// FILE: base.kt

interface Base<T>

// MODULE: intermediate(base)
// FILE: intermediate.kt

typealias BaseTA = Base<String>

// MODULE: main(base, intermediate)
// FILE: main.kt

interface MainInterface {
    fun f(): BaseTA // OK, because RHS of BaseTA is in library dependencies
}

/* GENERATED_FIR_TAGS: functionDeclaration, interfaceDeclaration, nullableType, typeAliasDeclaration, typeParameter */
