object O {
    val x = "OK"

    operator fun invoke() = x
}

typealias A = O

fun box(): String = A()
