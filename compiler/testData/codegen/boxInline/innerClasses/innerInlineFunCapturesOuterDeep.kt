// FILE: 1.kt
class O(val a: String) {
    inner class I1(val b: String) {
        inner class I2(val c: String) {
            inner class I3(val d: String) {
                inline fun foo(e: String) = "$a $b $c $d$e"
            }
        }
    }
}

// FILE: 2.kt
fun box(): String {
    val result = O("A").I1("lot").I2("of").I3("layers").foo("!")

    if (result != "A lot of layers!") return "fail: result is $result"

    return "OK"
}
