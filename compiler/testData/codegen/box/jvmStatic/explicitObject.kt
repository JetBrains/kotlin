// TARGET_BACKEND: JVM

// WITH_RUNTIME

object AX {

    @JvmStatic val c: String = "OK"

    @JvmStatic fun aStatic(): String {
        return AX.b()
    }

    fun aNonStatic(): String {
        return AX.b()
    }

    @JvmStatic fun b(): String {
        return "OK"
    }

    fun getProperty(): String {
        return AX.c
    }

}

fun box() : String {

    if (AX.aStatic() != "OK") return "fail 1"

    if (AX.aNonStatic() != "OK") return "fail 2"

    if (AX.getProperty() != "OK") return "fail 3"

    return "OK"
}
