import kotlin.platform.platformStatic

object AX {

    platformStatic fun aStatic(): String {
        return AX.b()
    }

    fun aNonStatic(): String {
        return AX.b()
    }

    platformStatic fun b(): String {
        return "OK"
    }

}

fun box() : String {

    if (AX.aStatic() != "OK") return "fail 1"

    if (AX.aNonStatic() != "OK") return "fail 1"

    return "OK"
}