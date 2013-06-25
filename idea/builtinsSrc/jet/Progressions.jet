package jet

public trait Progression<N: Any>: Iterable<N> {
    public val start: N
    public val end: N
    public val increment: Number
}

public class IntProgression(
    public override val start: Int,
    public override val end: Int,
    public override val increment: Int
): Progression<Int> {

    override fun iterator(): IntIterator
}

public class LongProgression(
    public override val start: Long,
    public override val end: Long,
    public override val increment: Long
): Progression<Long> {

    override fun iterator(): LongIterator
}

public class ByteProgression(
    public override val start: Byte,
    public override val end: Byte,
    public override val increment: Int
): Progression<Byte> {

    override fun iterator(): ByteIterator
}

public class ShortProgression(
    public override val start: Short,
    public override val end: Short,
    public override val increment: Int
): Progression<Short> {

    override fun iterator(): ShortIterator
}

public class CharProgression(
    public override val start: Char,
    public override val end: Char,
    public override val increment: Int
): Progression<Char> {

    override fun iterator(): CharIterator
}

public class FloatProgression(
    public override val start: Float,
    public override val end: Float,
    public override val increment: Float
): Progression<Float> {

    override fun iterator(): FloatIterator
}

public class DoubleProgression(
    public override val start: Double,
    public override val end: Double,
    public override val increment: Double
): Progression<Double> {

    override fun iterator(): DoubleIterator
}
