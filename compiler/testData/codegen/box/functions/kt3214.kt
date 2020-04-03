class A {
    fun get(vararg x: Int) = x.size
}

class B {
    fun get(vararg x: Unit) = x.size
}

fun test1(a: A): Int {
    return a.get(1)
}

fun test2(a: A): Int {
    return a.get(1, 2)
}

fun test3(b: B): Int {
    return b.get(Unit, Unit)
}


fun box() : String {
    var result = test1(A())
    if (result != 1) return "fail1: $result"

    result = test2(A())
    if (result != 2) return "fail2: $result"

    result = test3(B())
    if (result != 2) return "fail3: $result"

    return "OK"
}
