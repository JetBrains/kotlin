import kotlin.test.*

fun box() {
    val x: Array<String>? = null
    val y: Array<out String>? = null
    val xArray = x.orEmpty()
    val yArray = y.orEmpty()
    expect(0) { xArray.size }
    expect(0) { yArray.size }
}
