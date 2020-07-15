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