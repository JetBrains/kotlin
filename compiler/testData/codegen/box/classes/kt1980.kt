// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
public inline fun Int.times(body : () -> Unit) {
    var count = this;
    while (count > 0) {
       body()
       count--
    }
}

fun calc() : Int {
    val a = ArrayList<()->Int>()
    2.times {
        var j = 1
        a.add({ j })
        ++j
    }
    var sum = 0
    for (f in a) {
        val g = f as () -> Int
        sum += g()
    }
    return sum
}

fun box() : String {
    val x = calc()
    return if (x == 4) "OK" else x.toString()
}
