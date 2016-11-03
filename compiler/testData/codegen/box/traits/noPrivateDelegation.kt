// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

interface Z{

    private fun extension(): String {
        return "OK"
    }
}

object Z2 : Z {

}

fun box() : String {
    val size = Class.forName("Z2").declaredMethods.size
    if (size != 0) return "fail: $size"
    return "OK"
}