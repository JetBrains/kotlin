var result: String = "Fail"

operator fun Any.assign(other: String) {
    result = other
}

operator fun String.assign(x: Int) {}

operator fun String.set(i: Int, v: Int) {}
operator fun String.set(i: Int, v: Long) {}

operator fun Any.plusAssign(x: String) {}

operator fun String.plusAssign(x: String) {}
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

fun test_object_property_with_different_type(): String {
    val x = Foo("Hello")
    x.x = 5
    return result
}

fun test_assignment_type_mismatch(): String {
    val x = Foo("Hello")
    x.x = <!ASSIGNMENT_TYPE_MISMATCH!>5L<!>
    return result
}

fun test_set_priority(): String {
    val x = Foo("Hello")
    x.x[5] = 5
    x.x[5] = 5L
    return result
}

fun test_object_property_plus_assign(): String {
    val x = Foo("Hello")
    x.x += "OK"
    return result
}

fun test_null_safe_operator_with_assignment(): String {
    // TODO check if this is ok
    val x: Foo? = null
    x?.x = "OK"
    return result
}


fun test_plus_assign_assignment_type_mismatch(): String {
    val x = Foo("Hello")
    x.<!VAL_REASSIGNMENT!>x<!> += 5L
    return result
}
