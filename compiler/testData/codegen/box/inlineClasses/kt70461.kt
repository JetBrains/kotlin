// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS

interface Value2<out T> {
    val value: T

    fun isInstanceOf(): Boolean =
        when (this) {
            is DecimalW1 -> value.size == 5
            is DecimalW2 -> value.size == 5
            is DecimalW3 -> value as Int == 5
            is DecimalW4 -> value as UInt == 5U
            else -> true
        }
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class DecimalW1(override val value: List<Int>) : Value2<List<Int>>

OPTIONAL_JVM_INLINE_ANNOTATION
value class DecimalW2(override val value: ArrayList<Int>) : Value2<ArrayList<Int>>

OPTIONAL_JVM_INLINE_ANNOTATION
value class DecimalW3(override val value: Int) : Value2<Int>

OPTIONAL_JVM_INLINE_ANNOTATION
value class DecimalW4(override val value: UInt) : Value2<UInt>

fun box(): String {
    for (i in 0..10) {
        val list = List(i) { it }
        require(DecimalW1(list).isInstanceOf() == (i == 5))
        require(DecimalW2(ArrayList(list)).isInstanceOf() == (i == 5))
        require(DecimalW3(i).isInstanceOf() == (i == 5))
        require(DecimalW4(i.toUInt()).isInstanceOf() == (i == 5))
    }
    val another = object : Value2<Nothing> {
        override val value: Nothing get() = error(Any())
    }
    require(another.isInstanceOf())
    return "OK"
}
