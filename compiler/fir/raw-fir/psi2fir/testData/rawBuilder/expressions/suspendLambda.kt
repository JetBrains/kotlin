// LANGUAGE: +ParseLambdaWithSuspendModifier

fun testPositive() {
    suspend {}
    suspend { x -> }
    foo(suspend {})
    suspend l@ {}
    val x = @Ann suspend {}
    foo(@Ann suspend {})
}

fun testNegative() {
    suspend({})
    suspend() {}
    suspend("") {}
    suspend<String> {}
    x.suspend {}
    `suspend` {}
}