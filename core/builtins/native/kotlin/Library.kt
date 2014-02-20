package kotlin

public trait Annotation

public fun Any?.identityEquals(other: Any?): Boolean // = this === other

public fun String?.plus(other: Any?): String

public trait Comparable<in T> {
  public fun compareTo(other : T) : Int
}

public class Boolean private () : Comparable<Boolean> {
  public fun not() : Boolean

  public fun and(other : Boolean) : Boolean

  public fun or(other : Boolean) : Boolean

  public fun xor(other : Boolean) : Boolean

  public override fun compareTo(other : Boolean) : Int
}

public trait CharSequence {
  public fun get(index : Int) : Char

  public val length : Int
}

public class String() : Comparable<String>, CharSequence {
  public fun plus(other : Any?) : String

  public override fun compareTo(that : String) : Int
  public override fun get(index : Int) : Char
  public override val length: Int
}

public open class Throwable(message : String? = null, cause: Throwable? = null) {
    public fun getMessage() : String?
    public fun getCause() : Throwable?
    public fun printStackTrace() : Unit
}
