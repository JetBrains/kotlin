// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// WITH_STDLIB
// FILE: test.kt

import kotlin.test.assertEquals

val topLevelVal = ""
var topLevelVar = ""

val String.bar get() = "top"
val Foo.baz get() = "top"

class Foo {
    val memberVal = ""
    var memberVar = ""

    val bar = "member"
    private val baz = "member"

    companion object {
        fun referenceToMemberBaz() = Foo::baz
    }
}

fun box(): String {
    checkEqual(::topLevelVal, ::topLevelVal)
    checkEqual(::topLevelVar, ::topLevelVar)
    checkEqual(::topLevelVal, referenceTopLevelValFromOtherFile())
    checkEqual(::topLevelVar, referenceTopLevelVarFromOtherFile())

    checkEqual(Foo::memberVal, Foo::memberVal)
    checkEqual(Foo::memberVar, Foo::memberVar)
    checkEqual(Foo::memberVal, referenceMemberValFromOtherFile())
    checkEqual(Foo::memberVar, referenceMemberVarFromOtherFile())

    checkNotEqual(String::bar, Foo::bar)
    assertEquals("top", String::bar.get(""))
    assertEquals("member", Foo::bar.get(Foo()))

    checkNotEqual(Foo::baz, Foo.referenceToMemberBaz())
    assertEquals("top", Foo::baz.get(Foo()))
    assertEquals("member", Foo.referenceToMemberBaz().get(Foo()))

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

fun referenceTopLevelValFromOtherFile() = ::topLevelVal
fun referenceTopLevelVarFromOtherFile() = ::topLevelVar
fun referenceMemberValFromOtherFile() = Foo::memberVal
fun referenceMemberVarFromOtherFile() = Foo::memberVar

