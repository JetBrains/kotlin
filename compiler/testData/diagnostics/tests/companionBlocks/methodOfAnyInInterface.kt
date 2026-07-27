// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocks
// ISSUE: KT-88007

interface A {
    companion {
        fun <!METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE!>equals<!>(other: Any?): Boolean = true
    }
}

interface B {
    companion {
        fun <!METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE!>equals<!>(other: Any?): B = object : B {}
    }
}

interface C {
    companion {
        fun <!METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE!>hashCode<!>(): Int = 0
    }
}

interface D {
    companion {
        fun <!METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE!>hashCode<!>(): String = ""
    }
}

interface E {
    companion {
        fun <!METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE!>toString<!>(): String = "E"
    }
}

interface F {
    companion {
        fun <!METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE!>toString<!>(): CharSequence = "E"
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, functionDeclaration, interfaceDeclaration, nullableType */
