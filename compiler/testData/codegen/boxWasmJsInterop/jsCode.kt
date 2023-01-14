fun foo(x: Int, y: Int, z: String): Int = js("x + y + Number(z)")

val x: String = js("typeof 10")

fun box(): String {
    val res = foo(10, 20, z = "30")
    if (res != 60) return "Wrong foo: $res"
    if (x != "number") return "Wrong x: $x"

    return "OK"
}