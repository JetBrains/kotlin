// !LANGUAGE: +ReleaseCoroutines
// SKIP_TXT

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

    A().suspend {
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

    A() suspend {
        println()
    }

    A() suspend ({
        println()
    })

    A() suspend ""
}
