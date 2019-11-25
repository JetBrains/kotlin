// IGNORE_BACKEND_FIR: JVM_IR
var recivier : Any? = "fail"
var value2 : Any? = "fail2"

var <T> T.bar : T
    get() = this
    set(value) { recivier = this; value2 = value}


fun box(): String {
    String?::bar.set(null, null)
    if (recivier != null) "fail 1: ${recivier}"
    if (value2 != null) "fail 2: ${value2}"
    return "OK"
}