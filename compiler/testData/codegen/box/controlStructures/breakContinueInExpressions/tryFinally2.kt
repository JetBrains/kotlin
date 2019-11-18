// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    var r = ""
    for (i in 1..1)  {
        try {
            r += "O"
            break
        } finally {
            r += "K"
            continue
        }
    }
    return r
}
