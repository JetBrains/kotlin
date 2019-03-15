annotation class Simple(val value: String)

fun localCaptured(): Any {
    val z  = 1
    class A(@Simple("K") val z: String) {
        val x = z
    }
    return A("K")
}
