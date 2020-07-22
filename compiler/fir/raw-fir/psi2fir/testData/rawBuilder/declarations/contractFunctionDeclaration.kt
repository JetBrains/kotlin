contract fun bar(s: String?) = [returnsNotNull(), returns() implies (s != null)]

contract fun myContractFunction(str: String?, arg: MyClass, block: () -> Int) = [
    returns() implies (str != null),
    returns() implies (arg is MySubClass),
    callsInPlace(block, EXACTLY_ONCE)
]