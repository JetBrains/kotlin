package jet

public annotation class noinline
public annotation class inline(public val strategy: InlineStrategy = InlineStrategy.AS_FUNCTION)
public enum class InlineStrategy {
    AS_FUNCTION
    IN_PLACE
}
