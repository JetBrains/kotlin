// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -CompanionBlocksAndExtensions +ExplicitContextArguments
// WITH_STDLIB
class C1 {
    <!UNSUPPORTED_FEATURE!>companion<!> {}
}

class C2 {
    <!UNSUPPORTED_FEATURE!>companion<!> {
        fun foo() {}
    }
}

class C3 {
    <!UNSUPPORTED_FEATURE!>companion<!> {}
    companion {
        val bar = 1
    }
}

val x = object {
    <!ILLEGAL_COMPANION_BLOCK, UNSUPPORTED_FEATURE!>companion<!> {
        fun foo() {}
    }
}

enum class E {
    Entry {
        <!ILLEGAL_COMPANION_BLOCK, UNSUPPORTED_FEATURE!>companion<!> {
            fun foo() {}
        }
    };

    <!UNSUPPORTED_FEATURE!>companion<!> {
        context(s: String)
        val entries get() = listOf(E.Entry)

        context(s: String)
        fun values() = arrayOf(E.Entry)

        context(s: String)
        fun valueOf(x: String) = E.Entry
    }
}

fun test() {
    C2.<!UNSUPPORTED_FEATURE!>foo<!>()
    C3.<!UNSUPPORTED_FEATURE!>bar<!>

    E.Entry

    E.entries
    E.values()
    E.valueOf("Entry")

    E.<!UNSUPPORTED_FEATURE!>values<!>(s = "")
    E.<!UNSUPPORTED_FEATURE!>valueOf<!>("Entry", s = "")
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, propertyDeclaration */
