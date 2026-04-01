// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ExplicitContextArguments
// WITH_STDLIB
// DIAGNOSTICS: -PRE_RELEASE_CLASS

// MODULE: m1
// LANGUAGE: +CompanionBlocksAndExtensions
// FILE: m1.kt
class C {
    companion {
        fun foo() {}
        fun bar() = 1
        val baz = 1
        val qux = 1
    }

    companion object {
        fun bar() = ""
        val qux = ""
    }
}

enum class E {
    Entry;

    companion {
        context(s: String)
        val entries get() = listOf(E.Entry)

        context(s: String)
        fun values() = arrayOf(E.Entry)

        context(s: String)
        fun valueOf(x: String) = E.Entry
    }
}

// MODULE: m2(m1)
// LANGUAGE: -CompanionBlocksAndExtensions
// FILE: m2.kt

fun test() {
    C.<!UNSUPPORTED_FEATURE!>foo<!>()
    val x: String = C.bar() // resolves to companion object function

    C.<!UNSUPPORTED_FEATURE!>baz<!>
    val y: String = C.qux // resolves to companion object property

    E.Entry

    E.entries
    E.values()
    E.valueOf("Entry")

    E.<!UNSUPPORTED_FEATURE!>values<!>(s = "")
    E.<!UNSUPPORTED_FEATURE!>valueOf<!>("Entry", s = "")
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, propertyDeclaration */
