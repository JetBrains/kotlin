// IGNORE_BACKEND_FIR: JVM_IR
fun box() : String {
    val a = arrayOfNulls<Int>(5)
    var i = 0
    var sum = 0
    for(el in 0..4) {
       a[i] = i++
    }
    for (el in (a as Array<Int>)) {
        sum = sum + el
    }
    if(sum != 10) return "a failed"

    return "OK"
}
