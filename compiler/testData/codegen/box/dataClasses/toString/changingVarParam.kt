// IGNORE_BACKEND_FIR: JVM_IR
data class A(var string: String)

fun box(): String {
    val a = A("Fail")
    if(a.toString() != "A(string=Fail)") return "fail"

    a.string = "OK"
    if("$a" != "A(string=OK)") return a.toString()

    return "OK"
}
