// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +CompanionBlocks
// ISSUE: KT-88007
// LATEST_LV_DIFFERENCE

interface A {
    companion {
        <!ACCIDENTAL_OVERRIDE!>fun equals(other: Any?): Boolean<!> = true
    }
}

interface B {
    companion {
        fun equals(other: Any?): B = object : B {}
    }
}

interface C {
    companion {
        <!ACCIDENTAL_OVERRIDE!>fun hashCode(): Int<!> = 0
    }
}

interface D {
    companion {
        fun hashCode(): String = ""
    }
}

interface E {
    companion {
        <!ACCIDENTAL_OVERRIDE!>fun toString(): String<!> = "E"
    }
}

interface F {
    companion {
        fun toString(): CharSequence = "E"
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, functionDeclaration, interfaceDeclaration, nullableType */
