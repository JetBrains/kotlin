data class XYZ(val x: Int, val y: Int, val z: Int)

fun test() {
    val y = XYZ(1, 2, 3)./*rename*/component2()
}