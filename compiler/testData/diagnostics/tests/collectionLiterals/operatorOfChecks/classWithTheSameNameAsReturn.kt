// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80492
// LANGUAGE: +CollectionLiterals

// FILE: D.kt

package e

class E

// FILE: Main.kt

class C {
    class C
    companion object {
        operator fun of(): <!RETURN_TYPE_MISMATCH_OF_OPERATOR_OF!>C<!> = C()
        operator fun <!RETURN_TYPE_MISMATCH_OF_OPERATOR_OF!>of<!>(vararg str: String) = C()
    }
}

class D {
    companion object {
        operator fun <!RETURN_TYPE_MISMATCH_OF_OPERATOR_OF!>of<!>(vararg str: String) = D
    }
}

class E {
    companion object {
        operator fun of(vararg str: String): <!RETURN_TYPE_MISMATCH_OF_OPERATOR_OF!>e.E<!> = e.E()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, nestedClass, objectDeclaration, operator,
vararg */
