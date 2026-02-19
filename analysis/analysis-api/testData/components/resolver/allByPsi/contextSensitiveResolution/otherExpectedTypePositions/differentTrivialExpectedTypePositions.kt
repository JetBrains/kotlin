// ISSUE: KT-75316
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

enum class MyEnum {
    X, Y
}

val L = MyEnum.Y

val x: MyEnum = X
val y: MyEnum = L

fun myX(): MyEnum = X
fun myY(): MyEnum = L

fun foo(m: MyEnum = X) {}

val property1: MyEnum
    get() = X

val property2
    get(): MyEnum = X

fun main() {
    var m: MyEnum = X
    m = Y
    m = L
}

fun bar(b: Boolean): MyEnum {
    if (b) return X
    return L
}
