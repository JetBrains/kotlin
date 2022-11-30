// !FORCE_RENDER_ARGUMENTS
// !DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNREACHABLE_CODE
// Without `-UNREACHABLE_CODE` the test result for FE 1.0 will contain
// unbalanced tags in the last line of `testSetterProjectedOut`

fun <T> select(a: T, b: T) = a

fun testFunctionReturnSelectWithBlock(): String {
    return <!RETURN_TYPE_MISMATCH("kotlin/String; ILT: 1")!>select("test1", 1)<!>
}

fun testFunctionReturnIntWithBlock(): String {
    return <!RETURN_TYPE_MISMATCH("kotlin/String; kotlin/Int")!>2<!>
}

fun testFunctionNonLocalReturnSelect() = "test3".also {
    if (it == "test3+") {
        return <!RETURN_TYPE_MISMATCH("kotlin/String; it(kotlin/Comparable<*> & java/io/Serializable)")!>select("test3-", 3)<!>
    }
}

fun testFunctionNonLocalReturnInt() = "test4".also {
    if (it == "test4+") {
        return <!RETURN_TYPE_MISMATCH("kotlin/String; kotlin/Int")!>4<!>
    }
}

val testPropertyInitializerSelect: String = <!INITIALIZER_TYPE_MISMATCH("kotlin/String; ILT: 5")!>select("test5", 5)<!>

val testPropertyInitializerInt: String = <!INITIALIZER_TYPE_MISMATCH("kotlin/String; kotlin/Int")!>6<!>

fun testAssignment() {
    var it = "test7"
    it = <!ASSIGNMENT_TYPE_MISMATCH("kotlin/String; kotlin/Int")!>7<!>
    it = <!ASSIGNMENT_TYPE_MISMATCH("kotlin/String; ILT: 77")!>select("test7+", 77)<!>
}

class MyThingWithPlus(private val name: String) {
    operator fun plus(other: String) = 0
}

fun testPlusAssignment() {
    var it = MyThingWithPlus("test8")
    <!ASSIGNMENT_TYPE_MISMATCH("MyThingWithPlus; kotlin/Int")!>it += "test8-"<!>
    it <!NONE_APPLICABLE("[fun plus(other: String): Int, @IntrinsicConstEvaluation() fun plus(other: Byte): Int, @IntrinsicConstEvaluation() fun plus(other: Double): Double, ...]")!>+=<!> select("test8+", 88)
}

class MyThingWithPlusAssign(private val name: String) {
    operator fun plusAssign(other: String) {}
}

fun testPlusAssignAssignment() {
    var it = MyThingWithPlusAssign("test9")
    it += <!ARGUMENT_TYPE_MISMATCH("kotlin/String; kotlin/Int")!>9<!>
    it += <!ARGUMENT_TYPE_MISMATCH("kotlin/String; it(kotlin/Comparable<*> & java/io/Serializable)")!>select("test9+", 99)<!>
}

fun testAssignmentWithArray() {
    var it = arrayOf("test10")
    it[0] = <!ARGUMENT_TYPE_MISMATCH("kotlin/String; kotlin/Int")!>10<!>
    it[0] = <!ARGUMENT_TYPE_MISMATCH("kotlin/String; it(kotlin/Comparable<*> & java/io/Serializable)")!>select("test10+", 1010)<!>
}

fun testPlusAssignmentWithArray() {
    var it = arrayOf('!')
    it[0] += <!ARGUMENT_TYPE_MISMATCH("kotlin/Int; kotlin/String")!>"test11"<!>
    it[0] += <!ARGUMENT_TYPE_MISMATCH("kotlin/Int; it(kotlin/Comparable<it(kotlin/Char & kotlin/String)> & java/io/Serializable)")!>select('±', "test11+")<!>
}

class MyThingWithIncUnit(private val name: String) {
    <!INAPPLICABLE_OPERATOR_MODIFIER("receiver must be a supertype of the return type")!>operator<!> fun inc(): Unit {}
}

fun testWithMyThingWithIncUnit() {
    var it = MyThingWithIncUnit("test12")

    it = select(it, <!INC_DEC_SHOULD_NOT_RETURN_UNIT("")!>++<!>it)
    it = <!INC_DEC_SHOULD_NOT_RETURN_UNIT("")!>++<!>it
    <!INC_DEC_SHOULD_NOT_RETURN_UNIT("")!>++<!>it

    it = <!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION("T; kotlin/Unit, MyThingWithIncUnit; multiple incompatible classes")!>select<!>(it, it<!INC_DEC_SHOULD_NOT_RETURN_UNIT("")!>++<!>)
    it = it<!INC_DEC_SHOULD_NOT_RETURN_UNIT("")!>++<!>
    it<!INC_DEC_SHOULD_NOT_RETURN_UNIT("")!>++<!>
}

class MyThingWithIncString(private val name: String) {
    <!INAPPLICABLE_OPERATOR_MODIFIER("receiver must be a supertype of the return type")!>operator<!> fun inc() = "someString"
}

fun testWithMyThingWithIncString() {
    var it = MyThingWithIncString("test13")

    it = select(it, <!RESULT_TYPE_MISMATCH("MyThingWithIncString; kotlin/String")!>++it<!>)
    it = <!RESULT_TYPE_MISMATCH("MyThingWithIncString; kotlin/String")!>++it<!>
    <!RESULT_TYPE_MISMATCH("MyThingWithIncString; kotlin/String")!>++it<!>

    it = <!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION("T; kotlin/String, MyThingWithIncString; multiple incompatible classes")!>select<!>(it, <!RESULT_TYPE_MISMATCH("MyThingWithIncString; kotlin/String")!>it++<!>)
    it = <!RESULT_TYPE_MISMATCH("MyThingWithIncString; kotlin/String")!>it++<!>
    <!RESULT_TYPE_MISMATCH("MyThingWithIncString; kotlin/String")!>it++<!>
}

val testPropertyInitializerSelectWithNull: String = <!INITIALIZER_TYPE_MISMATCH("kotlin/String; kotlin/String?")!>select("test12", null)<!>

val testPropertyInitializerNull: String = <!NULL_FOR_NONNULL_TYPE("")!>null<!>

class MyHolder<T>(var value: T)

fun testSetterProjectedOut(it: MyHolder<*>) {
    <!SETTER_PROJECTED_OUT("value")!>it.value<!> = "test14"
    it.value = <!ASSIGNMENT_TYPE_MISMATCH("CapturedType(*); kotlin/String")!>select("test14+", throw Exception("test14-"))<!>
}
