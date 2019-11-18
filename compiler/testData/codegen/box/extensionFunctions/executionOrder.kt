// IGNORE_BACKEND_FIR: JVM_IR
var result = ""

fun getReceiver() : Int {
    result += "getReceiver->"
    return 1
}

fun getFun(b : Int.(Int)->Unit): Int.(Int)->Unit {
    result += "getFun()->"
    return b
}

fun box(): String {
    getReceiver().(getFun({ result +="End" }))(1)

    if(result != "getFun()->getReceiver->End") return "fail $result"

    return "OK"
}