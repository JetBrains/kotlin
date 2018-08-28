class _Class {
    val prop_1 = 1
    val prop_2 = 2
    val prop_3 = 3

    fun fun_1(): (Int) -> (Int) -> Int = {number: Int -> { number * 5 }}
    fun fun_2(value: Int): Int = value * 2
    fun fun_3(value1: Int): (Int) -> Int = fun(value2: Int): Int = value1 * value2 * 2

    operator fun contains(a: Int): Boolean = a > 30
    operator fun contains(a: Long): Boolean = a > 30L
    operator fun contains(a: Char): Boolean = a > 30.toChar()

    fun getIntArray(value: Int): IntArray = intArrayOf(1, 2, 3, value, 91923, 14, 123124)
    fun getLongArray(value: Long): LongArray = longArrayOf(1L, 2L, 3L, value, 9192323244L, 14L, 123124L)
    fun getCharArray(value: Char): CharArray = charArrayOf(1.toChar(), 2.toChar(), 3.toChar(), value)

    class _NestedClass {
        val prop_4 = 4
        val prop_5 = 5
    }
}

class _EmptyClass {}

class _ClassWithCompanionObject {
    companion object {}
}

open class _ClassLevel1 {}
open class _ClassLevel2: _ClassLevel1() {}
open class _ClassLevel3: _ClassLevel2() {}
open class _ClassLevel4: _ClassLevel3() {}
open class _ClassLevel5: _ClassLevel4() {}
class _ClassLevel6: _ClassLevel5() {}
