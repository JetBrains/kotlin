// IGNORE_BACKEND_FIR: JVM_IR
fun box() : String {
    val b : Array<Int?> = arrayOfNulls<Int> (5)
    var i = 0
    var sum = 0
    while(i < 5) {
       b[i] = i++
    }
    sum = 0
    for (el in b) {
        sum = sum + (el ?: 0)
    }
    if(sum != 10) return "b failed"

    return "OK"
}
