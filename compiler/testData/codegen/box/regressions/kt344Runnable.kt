// TARGET_BACKEND: JVM
// TARGET_BACKEND: JVM_IR
// WITH_STDLIB

fun t12(x: Int) : Int {
    var y = x
    val runnable = object : Runnable {
        override fun run () {
            y = y + 1
        }
    }
    while(y < 100) {
       runnable.run()
    }
    return y
}

fun box(): String {
    val result = t12(0)
    return if (result == 100) "OK" else result.toString()
}
