/**
* Represents a value which is either `true` or `false`. On the JVM, non-nullable values of this type are
* represented as values of the primitive type `boolean`.
*/
public interface Boolean {
    /**
    * Returns the inverse of this boolean.
    */
    public operator fun not(): Boolean

    /**
    * Performs a logical `and` operation between this Boolean and the [other] one.
    */
    infix fun and(other: Boolean): Boolean

    /**
    * Performs a logical `or` operation between this Boolean and the [other] one.
    */
    infix fun or(other: Boolean): Boolean

    /**
    * Performs a logical `xor` operation between this Boolean and the [other] one.
    */
    infix fun xor(other: Boolean): Boolean

    fun compareTo(other: Boolean): Int
}