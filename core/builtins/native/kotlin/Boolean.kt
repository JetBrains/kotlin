package kotlin

public class Boolean private () : Comparable<Boolean> {
    public fun not(): Boolean

    public fun and(other: Boolean): Boolean

    public fun or(other: Boolean): Boolean

    public fun xor(other: Boolean): Boolean

    public override fun compareTo(other: Boolean): Int
}
