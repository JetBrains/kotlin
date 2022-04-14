var result: String = "Fail"

operator fun Any.assign(other: String) {
    result = other
}

operator fun Any.plusAssign(x: String) {}
//operator fun Foo.plusAssign(x: Foo) {}
//operator fun Foo.plusAssign(x: String) {}

class Foo {
}


fun box(): String {
    val x = 10
    x = "OK"
    return result
}

fun test_2(): String {
    val x = 10
    x += "OK"
    return result
}
