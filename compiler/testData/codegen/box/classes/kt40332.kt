// IGNORE_BACKEND: JVM

// IGNORE_LIGHT_ANALYSIS
// ^ FUNCTION_EXPECTED: Expression 's' of type 'Any' cannot be invoked as a function. The function 'invoke()' is not found

object A {
    private val s = object {
        inline operator fun invoke(): String = "OK"
    }

    fun value() = s()
}

fun box(): String = A.value()
