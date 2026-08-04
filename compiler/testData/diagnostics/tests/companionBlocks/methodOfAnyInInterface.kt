// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +CompanionBlocks
// ISSUE: KT-88007

interface A {
    companion {
        fun equals(other: Any?): Boolean = true
    }
}

interface B {
    companion {
        fun equals(other: Any?): B = object : B {}
    }
}

interface C {
    companion {
        fun hashCode(): Int = 0
    }
}

interface D {
    companion {
        fun hashCode(): String = ""
    }
}

interface E {
    companion {
        fun toString(): String = "E"
    }
}

interface F {
    companion {
        fun toString(): CharSequence = "E"
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, functionDeclaration, interfaceDeclaration, nullableType */
