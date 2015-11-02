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