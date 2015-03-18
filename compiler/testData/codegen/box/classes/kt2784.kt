open class Factory(p: Int)

class A {
    companion object : Factory(1)
}

fun box() : String {
    A
    return "OK"
}