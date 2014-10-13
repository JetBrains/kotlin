import test.*

fun box(): String {
    var s1 = ""
    val s2 = Measurements().measure("K") {
        s1 = "O"
    }

    return s1 + s2
}
