// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

// MODULE: lib
// LANGUAGE: -CollectionLiterals
// FILE: decl.kt

@ExperimentalCollectionLiteralsApi
class MyExperimentalCollection

@ExperimentalCollectionLiteralsApi
open class MyExperimentalBase {
    @ExperimentalCollectionLiteralsApi
    open fun foo() {}
}

// MODULE: featureEnabled(lib)
// LANGUAGE: +CollectionLiterals
// FILE: featureEnabled.kt

fun useSiteWithFeature() {
    val p: MyExperimentalCollection = MyExperimentalCollection()
}

class InheritorWithFeature : MyExperimentalBase() {
    override fun foo() {}
}

// MODULE: featureDisabled(lib)
// LANGUAGE: -CollectionLiterals
// FILE: featureDisabled.kt

fun useSiteWithoutFeature() {
    val p: <!OPT_IN_USAGE_ERROR!>MyExperimentalCollection<!> = <!OPT_IN_USAGE_ERROR!>MyExperimentalCollection<!>()
}

class InheritorWithoutFeature : <!OPT_IN_USAGE_ERROR!>MyExperimentalBase<!>() {
    override fun <!OPT_IN_OVERRIDE_ERROR!>foo<!>() {}
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localProperty, override, propertyDeclaration */
