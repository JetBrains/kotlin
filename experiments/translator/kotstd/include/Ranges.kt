package kotlin.ranges

import kotlin.IntProgression
import kotlin.collections.IntIterator

public class IntRange(val start: Int, val endInclusive: Int) {
    val progression = IntProgression(start, endInclusive, 1)
    val first: Int
    val last: Int

    init {
        this.first = progression.first
        this.last = progression.last
    }

    fun contains(value: Int): Boolean = first <= value && value <= last

    fun isEmpty(): Boolean = first > last

    fun iterator(): IntIterator = progression.iterator()

    override fun hashCode(): Int =
            if (isEmpty()) -1 else (31 * first + last)

}
