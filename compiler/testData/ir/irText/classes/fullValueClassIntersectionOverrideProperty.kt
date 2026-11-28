// ISSUE: KT-70461, KT-91357
// LANGUAGE: +FullValueClasses

interface Value<out T> {
    val value: T

    // The type of `value` in the calls below depends on whether the smart cast is to an inline class
    fun sizeOfFull(): Int =
        when (this) {
            is Full -> value.size
            else -> -1
        }

    fun sizeOfInline(): Int =
        when (this) {
            is Inline -> value.size
            else -> -1
        }
}

value class Full(override val value: List<Int>) : Value<List<Int>>

inline value class Inline(override val value: List<Int>) : Value<List<Int>>
