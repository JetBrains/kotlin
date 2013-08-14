open class Factory(p: Int)

class A {
    class object : Factory(1)
}

fun box() : String {
    A
    return "OK"
}