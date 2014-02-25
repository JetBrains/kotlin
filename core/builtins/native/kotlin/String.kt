package kotlin

public class String : Comparable<String>, CharSequence {
    public fun plus(other: Any?): String

    public override fun compareTo(other: String): Int
    public override fun get(index: Int): Char
    public override val length: Int
}
