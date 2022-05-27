// WITH_STDLIB
var initialized = false

object O {
    val z = "OK"
    init { initialized = true }
}

val x by O::z

fun box(): String = if (initialized) x else "Fail"
