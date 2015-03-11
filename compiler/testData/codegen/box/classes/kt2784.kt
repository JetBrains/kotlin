open class Factory(p: Int)

class A {
    default object : Factory(1)
}

fun box() : String {
    A
    return "OK"
}