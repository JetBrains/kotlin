package jet

public fun arrayOfNulls<T>(size : Int) : Array<T?>

public class Array<reified T>(public val size : Int, init : (Int) -> T) : Iterable<T> {
  public fun get(index : Int) : T
  public fun set(index : Int, value : T) : Unit

  override public fun iterator() : Iterator<T>

  public val indices : IntRange
}

public class ByteArray(public val size : Int) : Iterable<Byte> {
  public fun get(index : Int) : Byte
  public fun set(index : Int, value : Byte) : Unit

  override public fun iterator() : ByteIterator

  public val indices : IntRange
}

public class ShortArray(public val size : Int) : Iterable<Short> {
  public fun get(index : Int) : Short
  public fun set(index : Int, value : Short) : Unit

  override public fun iterator() : ShortIterator

  public val indices : IntRange
}

public class IntArray(public val size : Int) : Iterable<Int> {
  public fun get(index : Int) : Int
  public fun set(index : Int, value : Int) : Unit

  override public fun iterator() : IntIterator

  public val indices : IntRange
}

public class LongArray(public val size : Int) : Iterable<Long> {
  public fun get(index : Int) : Long
  public fun set(index : Int, value : Long) : Unit

  override public fun iterator() : LongIterator

  public val indices : IntRange
}

public class FloatArray(public val size : Int) : Iterable<Float> {
  public fun get(index : Int) : Float
  public fun set(index : Int, value : Float) : Unit

  override public fun iterator() : FloatIterator

  public val indices : IntRange
}

public class DoubleArray(public val size : Int) : Iterable<Double> {
  public fun get(index : Int) : Double
  public fun set(index : Int, value : Double) : Unit

  override public fun iterator() : DoubleIterator

  public val indices : IntRange
}

public class CharArray(public val size : Int) : Iterable<Char> {
  public fun get(index : Int) : Char
  public fun set(index : Int, value : Char) : Unit

  override public fun iterator() : CharIterator

  public val indices : IntRange
}

public class BooleanArray(public val size : Int) : Iterable<Boolean> {
  public fun get(index : Int) : Boolean
  public fun set(index : Int, value : Boolean) : Unit

  override public fun iterator() : BooleanIterator

  public val indices : IntRange
}
