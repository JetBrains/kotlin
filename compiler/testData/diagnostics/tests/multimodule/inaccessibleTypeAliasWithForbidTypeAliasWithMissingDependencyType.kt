// LANGUAGE: +ForbidTypeAliasWithMissingDependencyType
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79781

// MODULE: base
// FILE: base.kt

interface BaseWithTypeParam<T>
interface BaseNoTypeParam

// MODULE: intermediate(base)
// FILE: intermediate.kt

typealias TAtoBaseWithTypeParam = BaseWithTypeParam<String>
typealias TAtoBaseNoTypeParam = BaseNoTypeParam

// MODULE: main(intermediate)
// FILE: main.kt

interface MainInterface {
    fun f(): TAtoBaseWithTypeParam // Error (BaseWithTypeParam is not in library dependencies)
    fun g(): TAtoBaseNoTypeParam // Error (BaseWithTypeParam is not in library dependencies)
}

/* GENERATED_FIR_TAGS: functionDeclaration, interfaceDeclaration, nullableType, typeAliasDeclaration, typeParameter */
