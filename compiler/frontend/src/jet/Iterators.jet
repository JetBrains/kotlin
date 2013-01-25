package jet

public trait Iterator<out T> {
  public fun next()  : T
  public fun hasNext() : Boolean
}

public trait MutableIterator<out T> : Iterator<T> {
  public fun remove()
}

public abstract class ByteIterator() : Iterator<Byte> {
  public abstract open fun nextByte() : Byte

  public override fun next() : Byte
}

public abstract class ShortIterator() : Iterator<Short> {
  public abstract open fun nextShort() : Short

  public override fun next() : Short
}

public abstract class CharIterator() : Iterator<Char> {
  public abstract open fun nextChar()  : Char

  public override fun next() : Char
}

public abstract class IntIterator() : Iterator<Int> {
  public abstract open fun nextInt()  : Int

  public override fun next() : Int
}

public abstract class LongIterator() : Iterator<Long> {
  public abstract open fun nextLong()  : Long

  public override fun next() : Long
}

public abstract class FloatIterator() : Iterator<Float> {
  public abstract open fun nextFloat()  : Float

  public override fun next() : Float
}

public abstract class DoubleIterator() : Iterator<Double> {
  public abstract open fun nextDouble()  : Double

  public override fun next() : Double
}

abstract open public class BooleanIterator() : Iterator<Boolean> {
  public abstract open fun nextBoolean()  : Boolean

  public override fun next() : Boolean
}

public fun <T> Iterator<T>.iterator() : Iterator<T>

public trait ListIterator<out T> : Iterator<T> {
    // Query Operations
    override fun hasNext() : Boolean
    override fun next() : T

    public fun hasPrevious() : Boolean
    public fun previous() : T
    public fun nextIndex() : Int
    public fun previousIndex() : Int
}

public trait MutableListIterator<T> : ListIterator<T>, MutableIterator<T> {
    // Query Operations
    override fun hasNext() : Boolean
    override fun next() : T

    // Modification Operations
    override fun remove()
    public fun set(e : T)
    public fun add(e : T)
}
