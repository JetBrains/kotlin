// !LANGUAGE: +ReleaseCoroutines
// SKIP_TXT

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

    kotlin.suspend {

    }

    suspend() {
        println()
    }

    suspend({ println() })

    suspend<Unit> {
        println()
    }

    val w: (suspend () -> Int) -> Any? = ::suspend
}

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class Ann

fun main(suspend: WLambdaInvoke) {

    suspend {}
}

class WLambdaInvoke {
    operator fun invoke(l: () -> Unit) {}
}
