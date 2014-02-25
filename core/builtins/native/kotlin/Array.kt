package kotlin

public class Array<reified T>(public val size: Int, init: Function1<Int, T>) {
    public fun get(index: Int): T
    public fun set(index: Int, value: T): Unit

    public fun iterator(): Iterator<T>

    public val indices: IntRange
}
