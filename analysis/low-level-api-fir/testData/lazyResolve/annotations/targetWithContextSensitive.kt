// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
class UseTarget {
    @Anno
    val pro<caret>p = 42
}

@Target(FIELD)
annotation class Anno
