// ISSUE: KT-75316
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: ANY
// WITH_REFLECT
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

enum class MyEnum {
    X, Y
}

annotation class A1(
    val value: MyEnum,
)

@A1(X)
fun foo1() {}

annotation class A2(
    vararg val value: MyEnum,
)

@A2(X, Y)
fun foo20() {}

@A2(*[X, Y])
fun foo21() {}

annotation class A3(
    val value: Array<MyEnum>,
)

@A3([X])
fun foo30() {}

@A3(arrayOf(X))
fun foo31() {}

annotation class A4(
    val value: Int,
)

@A4(MAX_VALUE)
fun foo4() {}

fun box(): String {
    val t1: A1 = ::foo1.annotations[0] as A1
    if (t1.value != MyEnum.X) return "fail 1"

    val t2: A2 = ::foo20.annotations[0] as A2
    if (!t2.value.contentEquals(arrayOf(MyEnum.X, MyEnum.Y))) return "fail 2"

    val t3: A2 = ::foo21.annotations[0] as A2
    if (!t3.value.contentEquals(arrayOf(MyEnum.X, MyEnum.Y))) return "fail 3"

    val t4: A3 = ::foo30.annotations[0] as A3
    if (!t4.value.contentEquals(arrayOf(MyEnum.X))) return "fail 4"

    val t5: A3 = ::foo31.annotations[0] as A3
    if (!t5.value.contentEquals(arrayOf(MyEnum.X))) return "fail 5"

    val t6: A4 = ::foo4.annotations[0] as A4
    if (t6.value != Int.MAX_VALUE) return "fail 6"

    return "OK"
}
