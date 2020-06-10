// PROBLEM: none
// WITH_RUNTIME
class TimeIndex(val intValue: Int) : Comparable<TimeIndex> {
    override fun compareTo(other: TimeIndex): Int {
        TODO()
    }

    operator fun rangeTo(other: TimeIndex): TimeIndexRange {
        return TimeIndexRange(this, other)
    }
}

data class TimeIndexRange(val start: TimeIndex, val end: TimeIndex) : Iterable<TimeIndex> {
    override fun iterator(): Iterator<TimeIndex> {
        TODO()
    }

    operator fun contains(index: TimeIndex): Boolean {
        return <caret>start <= index && index <= end
    }
}
