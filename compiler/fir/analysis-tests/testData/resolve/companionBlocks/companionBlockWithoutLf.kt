// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -CompanionBlocksAndExtensions
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
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, propertyDeclaration */
