// the following functions have type constraints and contracts written in different order
// any order is correct

// FILE: MyClass.kt
open class MyClass

// FILE: MyOtherClass.kt
open class MyOtherClass

// FILE: SuperType.kt
interface SuperType

// FILE: SomeType.kt
interface SomeType

// FILE: main.kt
fun <T, E> someFunctionWithTypeConstraints(arg: E?, block: () -> T): String
    contract [
        returns() implies (arg != null),
        callsInPlace(block, EXACTLY_ONCE),
    ]
    where T : MyClass,
          E : MyOtherClass
{
    block()
    arg ?: throw IllegalArgumentException()
    return "some string"
}

fun <D, T> anotherFunctionWithTypeConstraints(data: D?, arg: T?, block: () -> Unit)
    where D : SuperType,
          T : SomeType
    contract [
        returns() implies (data != null),
        returns() implies (arg != null)
    ]
{
    require(data != null)
    require(arg != null)
    block()
}