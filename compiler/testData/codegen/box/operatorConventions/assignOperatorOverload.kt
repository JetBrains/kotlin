import kotlin.reflect.KProperty

// !LANGUAGE:+AssignOperatorOverload

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

class ByDelegate {
    val v: Int by Delegate()
}

class Delegate {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
        return 5
    }
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
    }
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
    operator fun assign(value: Int) {
        result = "OK.operator.SelectAssignTest2.assign"
    }
    fun test() {
        val s = SelectAssignTest2()
        s = 1
    }
}
operator fun SelectAssignTest2.assign(i: Int) {
    result = "Fail.operator.SelectAssignTest2.assign"
}

class SelectAssignTest3 {
    fun assign(value: Int) {
        result = "Fail.SelectAssignTest3.assign"
    }
    fun test() {
        val s = SelectAssignTest3()
        s = 1
    }
}
operator fun SelectAssignTest3.assign(i: Int) {
    result = "OK.operator.SelectAssignTest3.assign"
}

class LambaContainer
operator fun LambaContainer.assign(r: Runnable): Unit {
    r.run()
}

data class FunctionReference(var result: String)
operator fun FunctionReference.assign(r: (FunctionReference) -> Unit): Unit {
    r.invoke(this)
}

data class GenericParameter(var result: String)
operator fun <T> GenericParameter.assign(v: T): Unit {
    this.result = "OK.GenericParameter.${v!!::class.simpleName}"
}

fun box(): String {
    // Test simple assign for local variable
    result = "Fail"
    val x = 10
    x = "OK"
    if (result != "OK") return "Fail: $result"

    // Test same type assign overload
    result = "Fail"
    x = 5
    if (result != "OK.Int.assign(Int)") return "Fail: $result"

    // Test assign overload for var is not applied
    result = "OK.var"
    var y = 10
    y = 5
    if (result != "OK.var" || y != 5) return "Fail: $result, y = $y"

    // Test simple assign for property
    result = "Fail"
    val foo = Foo(Container("Fail"))
    foo.x = 42
    if (foo.x.value != "OK") return "Fail: ${foo.x.value}"

    // Test set() has priority
    foo.x.value = "Fail"
    foo.x[1] = 2
    if (foo.x.value != "OK.Container.set1") return "Fail: ${foo.x.value}"
    foo.x[1] = 2L
    if (foo.x.value != "OK.Container.set2") return "Fail: ${foo.x.value}"

    // Test `operator String.assign` is selected over SelectAssignTest class method `String.assign` without `operator` keyword
    result = "Fail"
    SelectAssignTest().test()
    if (result != "OK.operator.String.assign") return "Fail: ${result}"

    // Test SelectAssignTest2 class method `operator assign` is selected over `operator assign` outside the class
    result = "Fail"
    SelectAssignTest2().test()
    if (result != "OK.operator.SelectAssignTest2.assign") return "Fail: ${result}"

    // Test `operator SelectAssignTest3.assign` is selected and not SelectAssignTest3 class `assign` method without `operator` keyword
    result = "Fail"
    SelectAssignTest3().test()
    if (result != "OK.operator.SelectAssignTest3.assign") return "Fail: ${result}"

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

    // Test that it works with delegate
    result = "Fail"
    val delegate: ByDelegate = ByDelegate()
    delegate.v = "OK"
    if (result != "OK") return "Fail: $result"

    // Test lambdas
    result = "Fail"
    val lambdaContainer = LambaContainer()
    lambdaContainer = { result = "OK.Runnable" }
    if (result != "OK.Runnable") return "Fail: $result"

    // Test function references
    val functionReference = FunctionReference(result = "Fail")
    val function: (FunctionReference) -> Unit = { it.result = "OK.FunctionReference" }
    functionReference = function
    if (functionReference.result != "OK.FunctionReference") return "Fail: ${functionReference.result}"

    // Test generic parameters
    val genericParameter = GenericParameter(result = "Fail")
    genericParameter = "string"
    if (genericParameter.result != "OK.GenericParameter.String") return "Fail: ${genericParameter.result}"
    genericParameter = 42
    if (genericParameter.result != "OK.GenericParameter.Int") return "Fail: ${genericParameter.result}"
    genericParameter = GenericParameter(result = "")
    if (genericParameter.result != "OK.GenericParameter.GenericParameter") return "Fail: ${genericParameter.result}"

    return "OK"
}
