var result: String = "Fail"

operator fun Any.assign(other: String) {
    result = other
}

operator fun Any.plusAssign(x: String) {}
//operator fun Foo.plusAssign(x: Foo) {}
//operator fun Foo.plusAssign(x: String) {}

data class Foo(val x: String)


fun test_local_variable(): String {
    val x = 10
    x = "OK"
    return result
}

fun test_local_variable_plus_assign(): String {
    val x = 10
    x += "OK"
    return result
}

fun test_object_property(): String {
    val x = Foo("Hello")
    x.x = "OK"
    return result
}

fun test_object_property_plus_assign(): String {
    val x = Foo("Hello")
    x.x += "OK"
    return result
}
