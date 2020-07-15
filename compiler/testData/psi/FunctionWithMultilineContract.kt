fun baz(num: Int, element: MyElement, block: () -> Unit): Int contract [
    callsInPlace(block, EXACTLY_ONCE),
    stringContract("some string"),
    someComplexContract(num, block, element)
] {
    block()
    if (num >= 0) {
        return 1;
    }
    return 0
}