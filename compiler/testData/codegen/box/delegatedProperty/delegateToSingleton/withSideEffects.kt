// WITH_STDLIB

var initialized = false

object O {
    init {
        initialized = true
    }

    operator fun getValue(x: Any?, y: Any?): String {
        return "value"
    }
}

class C {
    val s: String by O
}

fun box(): String {
    val c = C()
    return if (initialized) "OK" else "FAILURE"
}
