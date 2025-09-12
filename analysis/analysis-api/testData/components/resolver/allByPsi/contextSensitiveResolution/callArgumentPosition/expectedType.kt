// ISSUE: KT-75315
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

sealed interface SealedInterface {
    open class NestedInheritor(prop: String): SealedInterface {}
}

enum class MyEnum {
    EnumValue1;
}

open class MyClass {
    object InheritorObject: MyClass()
}

fun <X> id(x: X): X = x
fun <T> receive(arg: T) {}
fun <T: MyClass>testTypeParam(instance: T): String = TODO()

interface OtherI

fun <T>testTypeParamWithMultipleBound(instance: T): String where T : MyClass, T : OtherI = TODO()

fun testNullable(instance: MyClass?): String = TODO()

fun <T: MyClass?>testDefinitelyNotNullIntersection(instance: T & Any): String = TODO()

fun <T>testFakeIntersection(instance: T): String where T : SealedInterface, T : SealedInterface.NestedInheritor = TODO()

fun <T>testReverseFakeIntersection(instance: T): String where T : SealedInterface.NestedInheritor, T : SealedInterface = TODO()

fun <T>testRegularIntersection(instance: T): String where T : SealedInterface, T : MyClass = TODO()

fun testExpectedType() {
    val r1: MyEnum = id(EnumValue1)
    val r2: MyClass = id(InheritorObject)

    receive<MyEnum>(id(EnumValue1))
    receive<MyClass>(id(InheritorObject))

    testNullable(InheritorObject)
    testTypeParam<MyClass>(InheritorObject)

    testTypeParam(InheritorObject)
    testTypeParamWithMultipleBound(InheritorObject)
    testDefinitelyNotNullIntersection(InheritorObject)
    testFakeIntersection(InheritorObject)
    testReverseFakeIntersection(InheritorObject)
    testRegularIntersection(InheritorObject)
}
