// !FORCE_RENDER_ARGUMENTS
// !DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNREACHABLE_CODE
// Without `-UNREACHABLE_CODE` the test result for FE 1.0 will contain
// unbalanced tags in the last line of `testSetterProjectedOut`

fun <T> select(a: T, b: T) = a

fun testFunctionReturnSelectWithBlock(): String {
    return <!TYPE_MISMATCH!>select("test1", 1)<!>
}

fun testFunctionReturnIntWithBlock(): String {
    return <!CONSTANT_EXPECTED_TYPE_MISMATCH!>2<!>
}

fun testFunctionNonLocalReturnSelect() = "test3".also {
    if (it == "test3+") {
        return <!TYPE_MISMATCH!>select("test3-", 3)<!>
    }
}

fun testFunctionNonLocalReturnInt() = "test4".also {
    if (it == "test4+") {
        return <!TYPE_MISMATCH!>4<!>
    }
}

val testPropertyInitializerSelect: String = <!TYPE_MISMATCH!>select("test5", 5)<!>

val testPropertyInitializerInt: String = <!CONSTANT_EXPECTED_TYPE_MISMATCH!>6<!>

fun testAssignment() {
    var it = "test7"
    it = <!CONSTANT_EXPECTED_TYPE_MISMATCH!>7<!>
    it = <!TYPE_MISMATCH!>select("test7+", 77)<!>
}

class MyThingWithPlus(private val name: String) {
    operator fun plus(other: String) = 0
}

fun testPlusAssignment() {
    var it = MyThingWithPlus("test8")
    <!TYPE_MISMATCH!>it += "test8-"<!>
    <!TYPE_MISMATCH!>it += <!TYPE_MISMATCH!>select("test8+", 88)<!><!>
}

class MyThingWithPlusAssign(private val name: String) {
    operator fun plusAssign(other: String) {}
}

fun testPlusAssignAssignment() {
    var it = MyThingWithPlusAssign("test9")
    it += <!CONSTANT_EXPECTED_TYPE_MISMATCH!>9<!>
    it += <!TYPE_MISMATCH!>select("test9+", 99)<!>
}

fun testAssignmentWithArray() {
    var it = arrayOf("test10")
    it[0] = <!CONSTANT_EXPECTED_TYPE_MISMATCH!>10<!>
    it[0] = <!TYPE_MISMATCH!>select("test10+", 1010)<!>
}

fun testPlusAssignmentWithArray() {
    var it = arrayOf('!')
    it<!NO_SET_METHOD!>[0]<!> += <!TYPE_MISMATCH!>"test11"<!>
    it<!NO_SET_METHOD!>[0]<!> += <!TYPE_MISMATCH!>select('±', "test11+")<!>
}

class MyThingWithIncUnit(private val name: String) {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun inc(): Unit {}
}

fun testWithMyThingWithIncUnit() {
    var it = MyThingWithIncUnit("test12")

    it = select(it, <!INC_DEC_SHOULD_NOT_RETURN_UNIT!>++<!>it)
    it = <!INC_DEC_SHOULD_NOT_RETURN_UNIT!>++<!>it
    <!INC_DEC_SHOULD_NOT_RETURN_UNIT!>++<!>it

    it = select(it, it<!INC_DEC_SHOULD_NOT_RETURN_UNIT!>++<!>)
    it = it<!INC_DEC_SHOULD_NOT_RETURN_UNIT!>++<!>
    it<!INC_DEC_SHOULD_NOT_RETURN_UNIT!>++<!>
}

class MyThingWithIncString(private val name: String) {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun inc() = "someString"
}

fun testWithMyThingWithIncString() {
    var it = MyThingWithIncString("test13")

    it = <!TYPE_MISMATCH!>select(it, <!RESULT_TYPE_MISMATCH!>++<!>it)<!>
    it = <!TYPE_MISMATCH!><!RESULT_TYPE_MISMATCH!>++<!>it<!>
    <!RESULT_TYPE_MISMATCH!>++<!>it

    it = <!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING!>select<!>(it, it<!RESULT_TYPE_MISMATCH!>++<!>)
    it = it<!RESULT_TYPE_MISMATCH!>++<!>
    it<!RESULT_TYPE_MISMATCH!>++<!>
}

val testPropertyInitializerSelectWithNull: String = <!TYPE_MISMATCH!>select("test12", null)<!>

val testPropertyInitializerNull: String = <!NULL_FOR_NONNULL_TYPE!>null<!>

class MyHolder<T>(var value: T)

fun testSetterProjectedOut(it: MyHolder<*>) {
    <!SETTER_PROJECTED_OUT!>it.value<!> = "test14"
    it.value = <!TYPE_MISMATCH!>select("test14+", throw Exception("test14-"))<!>
}
