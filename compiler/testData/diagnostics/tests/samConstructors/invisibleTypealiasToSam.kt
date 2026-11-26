// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-92010

class X {
    fun interface Bar {
        fun bar(): Unit
    }

    private typealias Y = Bar

    private fun interface Z {
        fun foo(): Unit
    }
}

fun test() {
    X.Y { }
    X.<!INVISIBLE_REFERENCE!>Z<!> { }
}

/* GENERATED_FIR_TAGS: classDeclaration, funInterface, functionDeclaration, interfaceDeclaration, lambdaLiteral,
nestedClass, typeAliasDeclaration */
