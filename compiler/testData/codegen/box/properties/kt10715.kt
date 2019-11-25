// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    var a = Base()

    val count = a.count
    if (count != 0) return "fail 1: $count"

    val count2 = a.count
    if (count2 != 1) return "fail 2: $count2"

    return "OK"

}

class Base {
    var count: Int = 0
        get() = field++
}