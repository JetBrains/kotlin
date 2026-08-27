// WITH_STDLIB
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_FIRST_STAGE: Wasm-JS:2.0
// ISSUE: KT-70461
// LANGUAGE: +FullValueClasses

interface Value<out T> {
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

interface Value2_1<out T> {
    val value1: T

    fun isInstanceOf1(): Boolean =
        when (this) {
            is DecimalW1_2 -> value1.size == 5
            is DecimalW2_2 -> value1.size == 5
            is DecimalW3_2 -> value1 as Int == 5
            is DecimalW4_2 -> value1 as UInt == 5U
            else -> true
        }
}

interface Value2_2<out T> {
    val value2: T

    fun isInstanceOf2(): Boolean =
        when (this) {
            is DecimalW1_2 -> value2.size == 5
            is DecimalW2_2 -> value2.size == 5
            is DecimalW3_2 -> value2 as Int == 5
            is DecimalW4_2 -> value2 as UInt == 5U
            else -> true
        }
}

value class DecimalW1(override val value: List<Int>) : Value<List<Int>>

value class DecimalW2(override val value: ArrayList<Int>) : Value<ArrayList<Int>>

value class DecimalW3(override val value: Int) : Value<Int>

value class DecimalW4(override val value: UInt) : Value<UInt>

value class DecimalW1_2(override val value1: List<Int>, override val value2: List<Int>) : Value2_1<List<Int>>, Value2_2<List<Int>>

value class DecimalW2_2(override val value1: ArrayList<Int>, override val value2: ArrayList<Int>)
    : Value2_1<ArrayList<Int>>, Value2_2<ArrayList<Int>>

value class DecimalW3_2(override val value1: Int, override val value2: Int) : Value2_1<Int>, Value2_2<Int>

value class DecimalW4_2(override val value1: UInt, override val value2: UInt) : Value2_1<UInt>, Value2_2<UInt>

fun box(): String {
    for (i in 0..10) {
        val list = List(i) { it }
        require(DecimalW1(list).isInstanceOf() == (i == 5))
        require(DecimalW2(ArrayList(list)).isInstanceOf() == (i == 5))
        require(DecimalW3(i).isInstanceOf() == (i == 5))
        require(DecimalW4(i.toUInt()).isInstanceOf() == (i == 5))
        require(DecimalW1_2(list, list).isInstanceOf1() == (i == 5))
        require(DecimalW1_2(list, list).isInstanceOf2() == (i == 5))
        require(DecimalW2_2(ArrayList(list), ArrayList(list)).isInstanceOf1() == (i == 5))
        require(DecimalW2_2(ArrayList(list), ArrayList(list)).isInstanceOf2() == (i == 5))
        require(DecimalW3_2(i, i).isInstanceOf1() == (i == 5))
        require(DecimalW3_2(i, i).isInstanceOf2() == (i == 5))
        require(DecimalW4_2(i.toUInt(), i.toUInt()).isInstanceOf1() == (i == 5))
        require(DecimalW4_2(i.toUInt(), i.toUInt()).isInstanceOf2() == (i == 5))
    }
    val another1 = object : Value<Nothing> {
        override val value: Nothing get() = error(Any())
    }
    require(another1.isInstanceOf())
    val another2 = object : Value2_1<Nothing>, Value2_2<Nothing> {
        override val value1: Nothing get() = error(Any())
        override val value2: Nothing get() = error(Any())
    }
    require(another2.isInstanceOf1())
    require(another2.isInstanceOf2())
    return "OK"
}
