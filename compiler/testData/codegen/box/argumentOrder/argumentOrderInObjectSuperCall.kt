// IGNORE_BACKEND_FIR: JVM_IR
var result = "fail"

open class Base(val o: String, val k: String)

fun box(): String {
    val obj1 = object : Base(k = { result = "O"; "K"}() , o = {result += "K"; "O"}()) {}

    if (result != "OK") return "fail $result"
    return obj1.o + obj1.k
}