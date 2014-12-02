import kotlin.platform.platformStatic

object A {

    private platformStatic fun a(): String {
        return "OK"
    }

    object Z {
        val p = a()
    }
}

fun box(): String {
    return A.Z.p
}