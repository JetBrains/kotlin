// IntelliJ API Decompiler stub source generated from a class file
// Implementation of methods is not available

package kotlin.ranges

public open class CharProgression internal constructor(start: kotlin.Char, endInclusive: kotlin.Char, step: kotlin.Int) : kotlin.collections.Iterable<kotlin.Char> {
    public companion object {
        public final fun fromClosedRange(rangeStart: kotlin.Char, rangeEnd: kotlin.Char, step: kotlin.Int): kotlin.ranges.CharProgression { /* compiled code */ }
    }

    public final val first: kotlin.Char /* compiled code */

    public final val last: kotlin.Char /* compiled code */

    public final val step: kotlin.Int /* compiled code */

    public open operator fun iterator(): kotlin.collections.CharIterator { /* compiled code */ }

    public open fun isEmpty(): kotlin.Boolean { /* compiled code */ }

    public open operator fun equals(other: kotlin.Any?): kotlin.Boolean { /* compiled code */ }

    public open fun hashCode(): kotlin.Int { /* compiled code */ }

    public open fun toString(): kotlin.String { /* compiled code */ }
}

internal final class CharProgressionIterator public constructor(first: kotlin.Char, last: kotlin.Char, step: kotlin.Int) : kotlin.collections.CharIterator {
    public final val step: kotlin.Int /* compiled code */

    private final val finalElement: kotlin.Int /* compiled code */

    private final var hasNext: kotlin.Boolean /* compiled code */

    private final var next: kotlin.Int /* compiled code */

    public open operator fun hasNext(): kotlin.Boolean { /* compiled code */ }

    public open fun nextChar(): kotlin.Char { /* compiled code */ }
}

public final class CharRange public constructor(start: kotlin.Char, endInclusive: kotlin.Char) : kotlin.ranges.CharProgression, kotlin.ranges.ClosedRange<kotlin.Char>, kotlin.ranges.OpenEndRange<kotlin.Char> {
    public companion object {
        public final val EMPTY: kotlin.ranges.CharRange /* compiled code */
    }

    public open val start: kotlin.Char /* compiled code */
        public open get() { /* compiled code */ }

    public open val endInclusive: kotlin.Char /* compiled code */
        public open get() { /* compiled code */ }

    @kotlin.Deprecated @kotlin.SinceKotlin @kotlin.WasExperimental public open val endExclusive: kotlin.Char /* compiled code */
        public open get() { /* compiled code */ }

    public open operator fun contains(value: kotlin.Char): kotlin.Boolean { /* compiled code */ }

    public open fun isEmpty(): kotlin.Boolean { /* compiled code */ }

    public open operator fun equals(other: kotlin.Any?): kotlin.Boolean { /* compiled code */ }

    public open fun hashCode(): kotlin.Int { /* compiled code */ }

    public open fun toString(): kotlin.String { /* compiled code */ }
}

public interface ClosedRange<T : kotlin.Comparable<T>> {
    public abstract val start: T

    public abstract val endInclusive: T

    public open operator fun contains(value: T): kotlin.Boolean { /* compiled code */ }

    public open fun isEmpty(): kotlin.Boolean { /* compiled code */ }
}

public open class IntProgression internal constructor(start: kotlin.Int, endInclusive: kotlin.Int, step: kotlin.Int) : kotlin.collections.Iterable<kotlin.Int> {
    public companion object {
        public final fun fromClosedRange(rangeStart: kotlin.Int, rangeEnd: kotlin.Int, step: kotlin.Int): kotlin.ranges.IntProgression { /* compiled code */ }
    }

    public final val first: kotlin.Int /* compiled code */

    public final val last: kotlin.Int /* compiled code */

    public final val step: kotlin.Int /* compiled code */

    public open operator fun iterator(): kotlin.collections.IntIterator { /* compiled code */ }

    public open fun isEmpty(): kotlin.Boolean { /* compiled code */ }

    public open operator fun equals(other: kotlin.Any?): kotlin.Boolean { /* compiled code */ }

    public open fun hashCode(): kotlin.Int { /* compiled code */ }

    public open fun toString(): kotlin.String { /* compiled code */ }
}

internal final class IntProgressionIterator public constructor(first: kotlin.Int, last: kotlin.Int, step: kotlin.Int) : kotlin.collections.IntIterator {
    public final val step: kotlin.Int /* compiled code */

    private final val finalElement: kotlin.Int /* compiled code */

    private final var hasNext: kotlin.Boolean /* compiled code */

    private final var next: kotlin.Int /* compiled code */

    public open operator fun hasNext(): kotlin.Boolean { /* compiled code */ }

    public open fun nextInt(): kotlin.Int { /* compiled code */ }
}

public final class IntRange public constructor(start: kotlin.Int, endInclusive: kotlin.Int) : kotlin.ranges.IntProgression, kotlin.ranges.ClosedRange<kotlin.Int>, kotlin.ranges.OpenEndRange<kotlin.Int> {
    public companion object {
        public final val EMPTY: kotlin.ranges.IntRange /* compiled code */
    }

    public open val start: kotlin.Int /* compiled code */
        public open get() { /* compiled code */ }

    public open val endInclusive: kotlin.Int /* compiled code */
        public open get() { /* compiled code */ }

    @kotlin.Deprecated @kotlin.SinceKotlin @kotlin.WasExperimental public open val endExclusive: kotlin.Int /* compiled code */
        public open get() { /* compiled code */ }

    public open operator fun contains(value: kotlin.Int): kotlin.Boolean { /* compiled code */ }

    public open fun isEmpty(): kotlin.Boolean { /* compiled code */ }

    public open operator fun equals(other: kotlin.Any?): kotlin.Boolean { /* compiled code */ }

    public open fun hashCode(): kotlin.Int { /* compiled code */ }

    public open fun toString(): kotlin.String { /* compiled code */ }
}

public open class LongProgression internal constructor(start: kotlin.Long, endInclusive: kotlin.Long, step: kotlin.Long) : kotlin.collections.Iterable<kotlin.Long> {
    public companion object {
        public final fun fromClosedRange(rangeStart: kotlin.Long, rangeEnd: kotlin.Long, step: kotlin.Long): kotlin.ranges.LongProgression { /* compiled code */ }
    }

    public final val first: kotlin.Long /* compiled code */

    public final val last: kotlin.Long /* compiled code */

    public final val step: kotlin.Long /* compiled code */

    public open operator fun iterator(): kotlin.collections.LongIterator { /* compiled code */ }

    public open fun isEmpty(): kotlin.Boolean { /* compiled code */ }

    public open operator fun equals(other: kotlin.Any?): kotlin.Boolean { /* compiled code */ }

    public open fun hashCode(): kotlin.Int { /* compiled code */ }

    public open fun toString(): kotlin.String { /* compiled code */ }
}

internal final class LongProgressionIterator public constructor(first: kotlin.Long, last: kotlin.Long, step: kotlin.Long) : kotlin.collections.LongIterator {
    public final val step: kotlin.Long /* compiled code */

    private final val finalElement: kotlin.Long /* compiled code */

    private final var hasNext: kotlin.Boolean /* compiled code */

    private final var next: kotlin.Long /* compiled code */

    public open operator fun hasNext(): kotlin.Boolean { /* compiled code */ }

    public open fun nextLong(): kotlin.Long { /* compiled code */ }
}

public final class LongRange public constructor(start: kotlin.Long, endInclusive: kotlin.Long) : kotlin.ranges.LongProgression, kotlin.ranges.ClosedRange<kotlin.Long>, kotlin.ranges.OpenEndRange<kotlin.Long> {
    public companion object {
        public final val EMPTY: kotlin.ranges.LongRange /* compiled code */
    }

    public open val start: kotlin.Long /* compiled code */
        public open get() { /* compiled code */ }

    public open val endInclusive: kotlin.Long /* compiled code */
        public open get() { /* compiled code */ }

    @kotlin.Deprecated @kotlin.SinceKotlin @kotlin.WasExperimental public open val endExclusive: kotlin.Long /* compiled code */
        public open get() { /* compiled code */ }

    public open operator fun contains(value: kotlin.Long): kotlin.Boolean { /* compiled code */ }

    public open fun isEmpty(): kotlin.Boolean { /* compiled code */ }

    public open operator fun equals(other: kotlin.Any?): kotlin.Boolean { /* compiled code */ }

    public open fun hashCode(): kotlin.Int { /* compiled code */ }

    public open fun toString(): kotlin.String { /* compiled code */ }
}

@kotlin.SinceKotlin @kotlin.WasExperimental public interface OpenEndRange<T : kotlin.Comparable<T>> {
    public abstract val start: T

    public abstract val endExclusive: T

    public open operator fun contains(value: T): kotlin.Boolean { /* compiled code */ }

    public open fun isEmpty(): kotlin.Boolean { /* compiled code */ }
}
