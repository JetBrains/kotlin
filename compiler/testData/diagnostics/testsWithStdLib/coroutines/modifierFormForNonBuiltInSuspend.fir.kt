// !LANGUAGE: +ReleaseCoroutines
// SKIP_TXT

fun <R> suspend(block: suspend () -> R): suspend () -> R = block

class A {
    infix fun <R> suspend(block: suspend () -> R): suspend () -> R = block
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

    suspend<Unit> {
        println()
    }

    val w: (suspend () -> Int) -> Any? = ::suspend

    A().suspend {
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
        suspend {
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

    A() suspend {
        println()
    }

    A() suspend ({
        println()
    })
}
