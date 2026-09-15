// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

// MODULE: lib
// API_VERSION: 2.6
// FILE: decl.kt

@file:Suppress(<!ERROR_SUPPRESSION!>"INVISIBLE_REFERENCE"<!>)

@SinceKotlin("2.6")
@WasExperimental(ExperimentalCollectionLiteralsApi::class)
fun newlyStableApi() {}

@SinceKotlin("2.6")
@WasExperimental(ExperimentalCollectionLiteralsApi::class)
class NewlyStableCollection

// MODULE: featureEnabled(lib)
// LANGUAGE: +CollectionLiterals
// API_VERSION: 2.5
// FILE: featureEnabled.kt

@file:Suppress(<!ERROR_SUPPRESSION!>"PRE_RELEASE_CLASS"<!>)

fun useSiteWithFeature() {
    newlyStableApi()
    val p: NewlyStableCollection = NewlyStableCollection()
}

// MODULE: explicitOptIn(lib)
// LANGUAGE: -CollectionLiterals
// API_VERSION: 2.5
// OPT_IN: kotlin.ExperimentalCollectionLiteralsApi
// FILE: explicitOptIn.kt

@file:Suppress(<!ERROR_SUPPRESSION!>"PRE_RELEASE_CLASS"<!>)

fun useSiteWithExplicitOptIn() {
    newlyStableApi()
    val p: NewlyStableCollection = NewlyStableCollection()
}

// MODULE: noOptIn(lib)
// API_VERSION: 2.5
// LANGUAGE: -CollectionLiterals
// FILE: noOptIn.kt

@file:Suppress(<!ERROR_SUPPRESSION!>"PRE_RELEASE_CLASS"<!>)

fun useSiteWithoutOptIn() {
    <!OPT_IN_USAGE_ERROR!>newlyStableApi<!>()
    val p: <!OPT_IN_USAGE_ERROR!>NewlyStableCollection<!> = <!OPT_IN_USAGE_ERROR!>NewlyStableCollection<!>()
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classDeclaration, classReference, functionDeclaration, localProperty,
propertyDeclaration, stringLiteral */
