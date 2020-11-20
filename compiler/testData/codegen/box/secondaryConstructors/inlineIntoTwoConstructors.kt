inline fun myRun(x: () -> String) = x()

class C {
    val x = myRun { { "OK" }() }

    constructor(y: Int)
    constructor(y: String)
}

fun box(): String = C("").x
