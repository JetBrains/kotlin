
class OurTest : Test()

fun box(): String {
    val t = OurTest()
    val x: MutableCollection<String> = t

    if (t.size != 56) return "fail 1: ${t.size}"
    if (x.size != 56) return "fail 1: ${x.size}"

    return "OK"
}
