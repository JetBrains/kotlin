// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_VARIABLE
// WITH_STDLIB
// ISSUE: KT-68339

enum class ECCurve {
    A;

    val a by lazy {
        val bString = <!WHEN_ON_SEALED_GEEN_ELSE!>when (this) {
            A -> ""
        }<!>
    }

    val b by lazy {
        val bString = A
    }

    val c by lazy {
        fun local() {
            println(A)
        }
    }

    val d by lazy {
        class Local {
            fun foo() {
                println(A)
            }
        }
    }

    val e by lazy {
        object {
            fun foo() {
                println(A)
            }
        }
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, enumDeclaration, enumEntry, equalityExpression,
functionDeclaration, lambdaLiteral, localClass, localFunction, localProperty, propertyDeclaration, propertyDelegate,
stringLiteral, whenExpression, whenWithSubject */
