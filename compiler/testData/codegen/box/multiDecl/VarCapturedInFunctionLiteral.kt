class A {
    fun component1() = 1
    fun component2() = 2
}


fun box() : String {
    var (a, b) = A()

    val local = {
        a = 3
    }
    local()
    return if (a == 3 && b == 2) "OK" else "fail"
}