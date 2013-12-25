package jet

public trait Annotation

public annotation class volatile : Annotation
public annotation class atomic : Annotation
public annotation class data : Annotation
public annotation class deprecated(value: String) : Annotation
public annotation class suppress(vararg names: String)
public annotation class tailRecursive : Annotation

public annotation class noinline
public annotation class inline(public val strategy: InlineStrategy = InlineStrategy.AS_FUNCTION)
public enum class InlineStrategy {
    AS_FUNCTION
    IN_PLACE
}

public fun <R> synchronized(lock: Any, block : () -> R) : R

public fun Any?.identityEquals(other : Any?) : Boolean // = this === other

// Can't write a body due to a bootstrapping problem (see JET-74)
public fun Any?.equals(other : Any?) : Boolean// = this === other

// Returns "null" for null
public fun Any?.toString() : String// = this === other

public fun String?.plus(other: Any?) : String

public trait Comparable<in T> {
  public fun compareTo(other : T) : Int
}

public trait Hashable {
  public fun hashCode() : Int
  public fun equals(other : Any?) : Boolean
}

public class Boolean private () : Comparable<Boolean> {
  public fun not() : Boolean

  public fun and(other : Boolean) : Boolean

  public fun or(other : Boolean) : Boolean

  public fun xor(other : Boolean) : Boolean

  public override fun compareTo(other : Boolean) : Int

  public fun equals(other : Any?) : Boolean
}

public trait CharSequence {
  public fun get(index : Int) : Char

  public val length : Int

  public fun toString() : String
}

public class String() : Comparable<String>, CharSequence {
  public fun plus(other : Any?) : String

  public fun equals(other : Any?) : Boolean

  public override fun compareTo(that : String) : Int
  public override fun get(index : Int) : Char
  public override fun toString() : String
  public override val length: Int
}

public open class Throwable(message : String? = null, cause: Throwable? = null) {
    public fun getMessage() : String?
    public fun getCause() : Throwable?
    public fun printStackTrace() : Unit
}

public trait PropertyMetadata {
    public val name: String
}

/*
 * In front-end we need to resolve call PropertyMetadataImpl() in getter of delegated property
 * to be able generate it in back-end using ExpressionCodegen.invokeFunction
 */
public class PropertyMetadataImpl(public override val name: String): PropertyMetadata
