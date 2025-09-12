// ISSUE: KT-75316
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

@Retention(RUNTIME)
@Target(CLASS, PROPERTY)
annotation class MyAnnotation

@MyAnnotation
val x: Int = 1

@MyAnnotation
class MyClass

@MyAnnotation
fun myFunction() {}
