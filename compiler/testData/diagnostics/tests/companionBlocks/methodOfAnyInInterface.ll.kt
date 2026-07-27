// LL_FIR_DIVERGENCE
//   LL test doesn't report backend diagnostics
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +CompanionBlocks
// ISSUE: KT-88007
// LATEST_LV_DIFFERENCE

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
