package jet

public trait Range<in T: Comparable<T>> {
    public val start: T
    public val end: T
    public fun contains(item : T): Boolean
}

public class IntRange(
    public override val start: Int,
    public override val end : Int
) : Range<Int>, Progression<Int> {

    public override fun iterator() : IntIterator

    public override fun contains(item: Int): Boolean

    public override val increment: Int

    public class object {
        public val EMPTY: IntRange
    }
}

public class LongRange(
    public override val start: Long,
    public override val end: Long
) : Range<Long>, Progression<Long> {

    public override fun iterator(): LongIterator

    public override fun contains(item: Long): Boolean

    public override val increment: Long

    public class object {
        public val EMPTY: LongRange
    }
}

public class ByteRange(
    public override val start: Byte,
    public override val end : Byte
): Range<Byte>, Progression<Byte> {

    public override fun iterator(): ByteIterator

    public override fun contains(item: Byte): Boolean

    public override val increment: Int

    public class object {
        public val EMPTY: ByteRange
    }
}

public class ShortRange(
    public override val start: Short,
    public override val end: Short
) : Range<Short>, Progression<Short> {

    public override fun iterator(): ShortIterator

    public override fun contains(item: Short): Boolean

    public override val increment: Int

    public class object {
        public val EMPTY: ShortRange
    }
}

public class CharRange(
    public override val start: Char,
    public override val end: Char
) : Range<Char>, Progression<Char> {

    public override fun iterator(): CharIterator

    public override fun contains(item: Char) : Boolean

    public override val increment: Int

    public class object {
        public val EMPTY: CharRange
    }
}

public class FloatRange(
    public override val start: Float,
    public override val end: Float
) : Range<Float>, Progression<Float> {

    public override fun iterator(): FloatIterator

    public override fun contains(item: Float): Boolean

    public override val increment: Float

    public class object {
        public val EMPTY: FloatRange
    }
}

public class DoubleRange(
    public override val start: Double,
    public override val end: Double
) : Range<Double>, Progression<Double> {

    public override fun iterator() : DoubleIterator

    public override fun contains(item: Double): Boolean

    public override val increment: Double

    public class object {
        public val EMPTY: DoubleRange
    }
}
