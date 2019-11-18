// IGNORE_BACKEND_FIR: JVM_IR
fun box() : String {
    val data = Array<Array<Boolean>>(3) { Array<Boolean>(4, {false}) }
    for(d in data) {
        if(d.size != 4) return "fail"
        for(b in d) if (b) return "fail"
    }

    return "OK"
}
