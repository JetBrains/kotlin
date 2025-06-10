// LANGUAGE: +ParseLambdaWithSuspendModifier

fun testPositive() {
    suspend {}
    suspend { x -> }
    foo(suspend {})
    suspend l@ {}
}

fun testNegative() {
    suspend({})
    suspend() {}
    suspend("") {}
    suspend<String> {}
    x.suspend {}
}