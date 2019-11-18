// IGNORE_BACKEND_FIR: JVM_IR
fun box() : String {
    230?.toByte()?.hashCode()
    9.hashCode()

    if(230.equals(9.toByte())) {
       return "fail"
    }

    if(230 == 9.toByte().toInt()) {
       return "fail"
    }
    return "OK"
}
