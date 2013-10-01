class Z {}

fun box(): String {
    fun  Z.plus(s : String, d : String = "K") : String {
        return s + d
    }
    return Z() + "O"
}
