// FIR_IDENTICAL
// SKIP_TXT

fun <R> suspend(block: R) = block

class A {
    infix fun <R> suspend(block: R) = block
}

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class Ann

fun bar() {
    <!MODIFIER_FORM_FOR_NON_BUILT_IN_SUSPEND!>suspend<!> {
        println()
    }

    @Ann <!MODIFIER_FORM_FOR_NON_BUILT_IN_SUSPEND!>suspend<!> {
        println()
    }

    <!MODIFIER_FORM_FOR_NON_BUILT_IN_SUSPEND!>suspend<!> @Ann {
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
        <!MODIFIER_FORM_FOR_NON_BUILT_IN_SUSPEND!>suspend<!> {
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
