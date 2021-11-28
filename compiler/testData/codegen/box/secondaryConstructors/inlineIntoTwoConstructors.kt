inline fun myRun(x: () -> String) = x()

fun <T> eval(fn: () -> T) = fn()

class C {
    val x = myRun { eval { "OK" } }

    constructor(y: Int)
    constructor(y: String)
}

fun box(): String = C("").x
