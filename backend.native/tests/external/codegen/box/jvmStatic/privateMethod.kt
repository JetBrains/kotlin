// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME

object A {

    private @JvmStatic fun a(): String {
        return "OK"
    }

    object Z {
        val p = a()
    }
}

fun box(): String {
    return A.Z.p
}
