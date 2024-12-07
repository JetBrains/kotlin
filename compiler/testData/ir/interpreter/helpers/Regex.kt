package kotlin.text

public class Regex {
    public constructor(pattern: String) = TODO()

    public constructor(pattern: String, option: RegexOption) = TODO()

    public constructor(pattern: String, options: Set<RegexOption>) = TODO()

    public val pattern: String
        get() = TODO()

    public val options: Set<RegexOption>
        get() = TODO()

    public fun matchEntire(input: CharSequence): MatchResult? = TODO()

    public infix fun matches(input: CharSequence): Boolean = TODO()

    @SinceKotlin("1.7")
    @WasExperimental(ExperimentalStdlibApi::class)
    public fun matchAt(input: CharSequence, index: Int): MatchResult? = TODO()

    @SinceKotlin("1.7")
    @WasExperimental(ExperimentalStdlibApi::class)
    public fun matchesAt(input: CharSequence, index: Int): Boolean = TODO()

    public fun containsMatchIn(input: CharSequence): Boolean = TODO()

    public fun replace(input: CharSequence, replacement: String): String = TODO()

    public fun replace(input: CharSequence, transform: (MatchResult) -> CharSequence): String = TODO()

    public fun replaceFirst(input: CharSequence, replacement: String): String = TODO()

    public fun find(input: CharSequence, startIndex: Int = 0): MatchResult? = TODO()

    public fun findAll(input: CharSequence, startIndex: Int = 0): Sequence<MatchResult> = TODO()

    public fun split(input: CharSequence, limit: Int = 0): List<String> = TODO()

    @SinceKotlin("1.6")
    @WasExperimental(ExperimentalStdlibApi::class)
    public fun splitToSequence(input: CharSequence, limit: Int = 0): Sequence<String> = TODO()

    public companion object {
        public fun fromLiteral(literal: String): Regex = TODO()

        public fun escape(literal: String): String = TODO()

        public fun escapeReplacement(literal: String): String = TODO()
    }
}

public class MatchGroup {
    public val value: String
        get() = TODO()
}

public enum class RegexOption {
    IGNORE_CASE,

    MULTILINE
}
