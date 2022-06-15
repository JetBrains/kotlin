
// TARGET_BACKEND: JVM_IR
// !LANGUAGE:+AssignOperatorOverloadForJvmOldFrontend

var result: String = "Fail"

operator fun Int.assign(other: String) {
    result = other
}
operator fun Int.assign(other: Int) {
    result = "OK.Int.assign(Int)"
}
operator fun Container.assign(other: Int) {
    this.value = "OK"
}
operator fun Container.set(i: Int, v: Int) {
    this.value = "OK.Container.set1"
}
operator fun Container.set(i: Int, v: Long) {
    this.value = "OK.Container.set2"
}

data class Foo(val x: Container)
data class Container(var value: String)

data class NullCheck(val x: NullCheckContainer)
data class NullCheckContainer(var value: String)
operator fun NullCheckContainer?.assign(value: String) {
    result = value
}

operator fun String.assign(value: Int) {
    result = "OK.operator.String.assign"
}

class SelectAssignTest {
    fun String.assign(value: Int) {
        result = "Fail.String.assign"
    }

    fun test() {
        val s = "hello"
        s = 1
    }
}

class SelectAssignTest2 {
    fun assign(value: Int) {
        result = "Fail.SelectAssignTest2.assign"
    }

    fun test() {
        val s = SelectAssignTest2()
        s = 1
    }
}

operator fun SelectAssignTest2.assign(i: Int) {
    result = "OK.operator.SelectAssignTest2.assign"
}

fun box(): String {
    // Test simple assign for local variable
    val x = 10
    x = "OK"
    if (result != "OK") return "Fail: $result"

    // Test same type assign overload
    x = 5
    if (result != "OK.Int.assign(Int)") return "Fail: $result"

    // Test assign overload for var is not applied
    result = "OK.var"
    var y = 10
    y = 5
    if (result != "OK.var" || y != 5) return "Fail: $result, y = $y"

    // Test simple assign for property
    val foo = Foo(Container("Fail"))
    foo.x = 42
    if (foo.x.value != "OK") return "Fail: ${foo.x.value}"

    // Test set() has priority
    foo.x[1] = 2
    if (foo.x.value != "OK.Container.set1") return "Fail: ${foo.x.value}"
    foo.x[1] = 2L
    if (foo.x.value != "OK.Container.set2") return "Fail: ${foo.x.value}"

    // Test operator String.assign is selected over SelectAssignTest.String.assign
    result = "Fail"
    SelectAssignTest().test()
    if (result != "OK.operator.String.assign") return "Fail: ${result}"

    // Test operator SelectAssignTest2.assign is selected over SelectAssignTest2.assign
    result = "Fail"
    SelectAssignTest2().test()
    if (result != "OK.operator.SelectAssignTest2.assign") return "Fail: ${result}"

    // Test reference on null is not called
    result = "OK"
    val nullCheck: NullCheck? = null
    nullCheck?.x = "Fail"
    if (result != "OK") return "Fail: $result"

    // Test direct null is called
    result = "Fail"
    val nullCheckContainer: NullCheckContainer? = null
    nullCheckContainer = "OK"
    if (result != "OK") return "Fail: $result"

    return "OK"
}
