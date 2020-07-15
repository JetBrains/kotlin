contract fun myContractFunction(str: String?, arg: MyClass, block: () -> Int) = [
    returns() implies (str != null),
    returns() implies (arg is MySubClass),
    callsInPlace(block, EXACTLY_ONCE)
]
