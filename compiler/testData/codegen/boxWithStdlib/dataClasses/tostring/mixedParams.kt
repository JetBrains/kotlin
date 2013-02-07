data class A(var x: Int, y: Int, val z: Int?)

fun box(): String {
    val a = A(1, 2, null)
    if("$a" != "A(x=1, z=null)") return "$a"
    return "OK"
}
