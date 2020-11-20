fun baz(num: Int?, element: MyElement, block: () -> Unit): Int contract [
    callsInPlace(block, InvocationKind.EXACTLY_ONCE),
    returns() implies (num != null),
    returns() implies (element != null)
] {
    require(num != null)
    require(element != null)
    block()
    if (num >= 0) {
        return 1;
    }
    return 0
}
