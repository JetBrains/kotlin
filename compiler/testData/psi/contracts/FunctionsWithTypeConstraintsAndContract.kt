// the following functions have type constraints and contracts written in different order
// any order is correct

fun someFunctionWithTypeConstraints<T, E>(arg: E?, block: () -> T): String
    contract [
        returns() implies (arg != null),
        callsInPlace(block, EXACTLY_ONCE),
    ]
    where T : MyClass,
          E : MyOtherClass
{
    block()
    arg ?: throw NullArgumentException()
    return "some string"
}

fun anotherFunctionWithTypeConstraints<D, T>(data: D?, arg: T?, block: () -> Unit)
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