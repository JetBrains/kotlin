open class A(val result: String) {
    constructor(x: Int = 11, y: Int = 22, z: Int = 33) : this("$x$y$z")
}

class B() : A(y = 44)

fun box(): String {
    val result = B().result
    if (result != "114433") return "fail: $result"
    return "OK"
}