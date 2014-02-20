package kotlin

public class Char private () : Comparable<Char> {
    public fun compareTo(other : Double) : Int
    public fun compareTo(other : Float)  : Int
    public fun compareTo(other : Long)    : Int
    public fun compareTo(other : Int)    : Int
    public fun compareTo(other : Short)  : Int
    public override fun compareTo(other : Char)  : Int
    public fun compareTo(other : Byte)   : Int

    public fun plus(other : Double) : Double
    public fun plus(other : Float)  : Float
    public fun plus(other : Long)   : Long
    public fun plus(other : Int)    : Int
    public fun plus(other : Short)  : Int
    public fun plus(other : Byte)   : Int
    //  public fun plus(other : Char)   : Int

    public fun minus(other : Double) : Double
    public fun minus(other : Float)  : Float
    public fun minus(other : Long)   : Long
    public fun minus(other : Int)    : Int
    public fun minus(other : Short)  : Int
    public fun minus(other : Byte)   : Int
    public fun minus(other : Char)   : Int

    public fun times(other : Double) : Double
    public fun times(other : Float)  : Float
    public fun times(other : Long)   : Long
    public fun times(other : Int)    : Int
    public fun times(other : Short)  : Int
    public fun times(other : Byte)   : Int
    //  public fun times(other : Char)   : Int

    public fun div(other : Double) : Double
    public fun div(other : Float)  : Float
    public fun div(other : Long)   : Long
    public fun div(other : Int)    : Int
    public fun div(other : Short)  : Int
    public fun div(other : Byte)   : Int
    //  public fun div(other : Char)   : Int

    public fun mod(other : Double) : Double
    public fun mod(other : Float)  : Float
    public fun mod(other : Long)   : Long
    public fun mod(other : Int)    : Int
    public fun mod(other : Short)  : Int
    public fun mod(other : Byte)   : Int
    //  public fun mod(other : Char)   : Int

    public fun rangeTo(other : Char)   : CharRange

    public fun inc() : Char
    public fun dec() : Char
    public fun plus() : Int
    public fun minus() : Int

    public fun toDouble() : Double
    public fun toFloat() : Float
    public fun toLong() : Long
    public fun toInt() : Int
    public fun toChar() : Char
    public fun toShort() : Short
    public fun toByte() : Byte
}
