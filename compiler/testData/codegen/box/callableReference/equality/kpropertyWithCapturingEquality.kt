// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// FILE: test.kt

val topLevelVal = ""
var topLevelVar = ""

class Foo {
    val memberVal = ""
    var memberVar = ""
}

fun box(): String {
    val foo0 = Foo()
    val foo1 = Foo()

    checkEqual(foo0::memberVal, foo0::memberVal)
    checkEqual(foo0::memberVal, referenceMemberValFromOtherFile(foo0))
    checkEqual(foo0::memberVar, foo0::memberVar)
    checkEqual(foo0::memberVar, referenceMemberVarFromOtherFile(foo0))

    return "OK"
}

fun checkEqual(x: Any, y: Any) {
    if (x != y || y != x) throw AssertionError("$x and $y should be equal")
    if (x.hashCode() != y.hashCode()) throw AssertionError("$x and $y should have the same hash code")
}

fun checkNotEqual(x: Any, y: Any) {
    if (x == y || y == x) throw AssertionError("$x and $y should NOT be equal")
}

// FILE: otherFile.kt

fun referenceMemberValFromOtherFile(foo: Foo) = foo::memberVal
fun referenceMemberVarFromOtherFile(foo: Foo) = foo::memberVar

