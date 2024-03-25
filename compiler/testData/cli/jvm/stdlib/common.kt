// ISSUE: KT-65841

package kotlin

expect open class Any {
    public open operator fun equals(other: Any?): Boolean

    public open fun hashCode(): Int

    public open fun toString(): String
}

expect class Boolean

expect class Int

expect class String
