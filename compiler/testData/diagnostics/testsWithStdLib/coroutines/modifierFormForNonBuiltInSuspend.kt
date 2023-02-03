// FIR_IDENTICAL
// SKIP_TXT

fun <R> suspend(block: suspend () -> R): suspend () -> R = block

class A {
    infix fun <R> suspend(block: suspend () -> R): suspend () -> R = block
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

    suspend<Unit> {
        println()
    }

    val w: (suspend () -> Int) -> Any? = ::suspend

    A().<!MODIFIER_FORM_FOR_NON_BUILT_IN_SUSPEND!>suspend<!> {
        println()
    }

    A().suspend() {
        println()
    }

    A().suspend({ println() })

    A().suspend<Unit> {
        println()
    }

    with(A()) {
        <!MODIFIER_FORM_FOR_NON_BUILT_IN_SUSPEND!>suspend<!> {
            println()
        }

        suspend() {
            println()
        }

        suspend({ println() })

        suspend<Unit> {
            println()
        }
    }

    A() <!MODIFIER_FORM_FOR_NON_BUILT_IN_SUSPEND!>suspend<!> {
        println()
    }

    A() suspend ({
        println()
    })
}
