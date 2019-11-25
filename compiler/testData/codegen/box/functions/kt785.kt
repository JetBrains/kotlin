// IGNORE_BACKEND_FIR: JVM_IR
class A() {
    var x : Int = 0

    var z = {
        x++
    }
}

fun box() : String {
    val a = A()
    a.z()  //problem is here
    return if (a.x == 1) "OK" else "fail"
}
