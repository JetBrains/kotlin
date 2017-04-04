import kotlin.test.*

fun box() {
    val x: Array<String>? = arrayOf("1", "2")
    val xArray = x.orEmpty()
    expect(2) { xArray.size }
    expect("1") { xArray[0] }
    expect("2") { xArray[1] }
}
