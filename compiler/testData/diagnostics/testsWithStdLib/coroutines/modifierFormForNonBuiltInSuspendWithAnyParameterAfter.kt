// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// SKIP_TXT
// LANGUAGE: +ParseLambdaWithSuspendModifier

fun <R> suspend(block: R) = block

class A {
    infix fun <R> suspend(block: R) = block
}

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class Ann

fun bar() {
    suspend {
        println()
    }

    @Ann suspend {
        println()
    }

    suspend @Ann {
        println()
    }

    suspend() {
        println()
    }

    suspend({ println() })

    suspend<suspend () -> Unit> {
        println()
    }

    suspend<Nothing?>(null)

    val w: (Any?) -> Any? = ::suspend

    A().<!MODIFIER_FORM_FOR_NON_BUILT_IN_SUSPEND!>suspend<!> {
        println()
    }

    A().suspend() {
        println()
    }

    A().suspend({ println() })

    A().suspend<suspend () -> Unit> {
        println()
    }

    A().suspend<Nothing?>(null)

    with(A()) {
        suspend {
            println()
        }

        suspend() {
            println()
        }

        suspend({ println() })

        suspend<suspend () -> Unit> {
            println()
        }

        suspend<Nothing?>(null)
    }

    A() <!MODIFIER_FORM_FOR_NON_BUILT_IN_SUSPEND!>suspend<!> {
        println()
    }

    A() suspend ({
        println()
    })

    A() suspend ""
}

/* GENERATED_FIR_TAGS: annotationDeclaration, callableReference, classDeclaration, functionDeclaration, functionalType,
infix, lambdaLiteral, localProperty, nullableType, propertyDeclaration, stringLiteral, suspend, typeParameter */
