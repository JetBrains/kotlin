// TARGET_BACKEND: JVM
// WITH_STDLIB
class F : (Int) -> Int {
    operator fun invoke(): Int = -1
    override fun invoke(x: Int): Int = x * 2
}
fun box(): String {
    val f = F()
    val r = sequenceOf(1, 2).map(f).toList()
    return if (r == listOf(2, 4)) "OK" else "Fail: $r"
}
