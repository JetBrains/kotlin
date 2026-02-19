// LANGUAGE: -ForbidTypeAliasWithMissingDependencyType
// RENDER_DIAGNOSTICS_FULL_TEXT
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
    fun f(): TAtoBaseWithTypeParam // Error because incorrect type parameters deserialization could cause severe bugs like KT-79633. So, it's better to prohibit it ASAP.
    fun g(): TAtoBaseNoTypeParam // Warning that will become a error
}

/* GENERATED_FIR_TAGS: functionDeclaration, interfaceDeclaration, nullableType, typeAliasDeclaration, typeParameter */
