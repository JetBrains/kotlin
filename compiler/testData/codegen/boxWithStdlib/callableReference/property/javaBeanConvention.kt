// Name of the getter should be 'getaBcde' according to JavaBean conventions
var aBcde: Int = 239

fun box(): String {
    val x = (::aBcde).get()
    if (x != 239) return "Fail x: $x"

    (::aBcde).set(42)

    val y = (::aBcde).get()
    if (y != 42) return "Fail y: $y"

    return "OK"
}
