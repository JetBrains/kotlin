// IGNORE_BACKEND_FIR: JVM_IR
public fun box() : String {
    var i : Int?
    i = 10
    // Prefix increment on a smart cast should work
    val j = ++i

    return if (j == 11 && 11 == i) "OK" else "fail j = $j i = $i"
}
