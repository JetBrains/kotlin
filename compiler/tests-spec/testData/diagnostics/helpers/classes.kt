class Class {
    val prop_1 = 1
    val prop_2 = 2
    val prop_3 = 3
    val prop_4: Float? = 3f
    val prop_5: Float = 3f
    val prop_6: String = "..."
    val prop_7: Nothing? = "..."

    fun fun_1(): (Int) -> (Int) -> Int = {number: Int -> { number * 5 }}
    fun fun_2(value_1: Int): Int = value_1 * 2
    fun fun_3(value_1: Int): (Int) -> Int = fun(value_2: Int): Int = value_1 * value_2 * 2

    operator fun contains(a: Int) = a > 30
    operator fun contains(a: Long) = a > 30L
    operator fun contains(a: Char) = a > 30.toChar()

    fun getIntArray() = intArrayOf(1, 2, 3, 4, 5)
    fun getLongArray() = longArrayOf(1L, 2L, 3L, 4L, 5L)
    fun getCharArray() = charArrayOf(1.toChar(), 2.toChar(), 3.toChar(), 4.toChar(), 5.toChar())

    class _NestedClass {
        val prop_4 = 4
        val prop_5 = 5
    }
}

class EmptyClass {}

class ClassWithCompanionObject {
    companion object {}
}

open class ClassLevel1 {
    fun test1() {}
}
open class ClassLevel2: ClassLevel1() {
    fun test2() {}
}
open class ClassLevel3: ClassLevel2() {
    fun test3() {}
}
open class ClassLevel4: ClassLevel3() {
    fun test4() {}
}
open class ClassLevel5: ClassLevel4() {
    fun test5() {}
}
class ClassLevel6: ClassLevel5() {
    fun test6() {}
}

class Inv<T>(val x: T = null as T) {
    fun test() {}
    fun get() = x
    fun put(x: T) {}
    fun getNullable(): T? = select(x, null)
}

class In<in T>() {
    fun put(x: T) {}
    fun <K : T> getWithUpperBoundT(): K = x <!UNCHECKED_CAST!>as K<!>
}

class Out<out T>(val x: T = null as T) {
    fun get() = x
}

open class ClassWithTwoTypeParameters<K, L> {
    fun test1(): T? { return null }
    fun test2(): K? { return null }
}

class ClassWithThreeTypeParameters<K, L, M>(
    val x: K,
    val y: L,
    val z: M
)

class ClassWithSixTypeParameters<K, in L, out M, O, in P, out R>
