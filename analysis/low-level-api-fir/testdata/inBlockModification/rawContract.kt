fun foo(arg: Any?, num: Int?, block: () -> Unit) contract [
    returns() implies (arg is String),
    returns() implies (num != null),
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
] {
    require(arg is String)
    require(num != null)
    <expr>block</expr>()
}