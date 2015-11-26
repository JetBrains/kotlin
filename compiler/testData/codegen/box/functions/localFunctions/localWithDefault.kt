class Z {}

fun box(): String {
    operator fun Z.plus(s : String, d : String = "K") : String {
        return s + d
    }
    return Z() + "O"
}
