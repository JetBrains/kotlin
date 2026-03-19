// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-48183

// KT-48183: DeprecatedByOverridden i18n - non-i18n string in DeprecatedByOverridden deprecation message

interface Base {
    @Deprecated("old")
    fun foo()
}

class Derived : Base {
    override fun <!OVERRIDE_DEPRECATION!>foo<!>() {}
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, override, stringLiteral */
