// IGNORE_BACKEND: JVM

object A {
    private val s = object {
        inline operator fun invoke(): String = "OK"
    }

    fun value() = s()
}

fun box(): String = A.value()
