// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-32063
// RENDER_DIAGNOSTICS_FULL_TEXT

// KT-32063: RETURN_TYPE_MISMATCH_ON_OVERRIDE diagnostic reports wrong location of overridden function in multilevel hierarchy

class Foo : Bar() {
    override fun baz(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>Int<!> = 42
}

open class Bar : Baz()

open class Baz {
    open fun baz(): String = "Hello"
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, override, stringLiteral */
