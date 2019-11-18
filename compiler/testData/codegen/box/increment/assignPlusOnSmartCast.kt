// IGNORE_BACKEND_FIR: JVM_IR
public fun box() : String {
    var i : Int?
    i = 10
    // assignPlus on a smart cast should work
    i += 1

    return if (11 == i) "OK" else "fail i = $i"
}
