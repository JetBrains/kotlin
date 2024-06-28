// TARGET_BACKEND: JVM
// WITH_STDLIB
// JVM_ABI_K1_K2_DIFF: KT-63828

import java.util.Arrays

class MyCollection<T>(val delegate: Collection<T>): Collection<T> by delegate {
    public fun toArray(): Array<Any?> {
        val a = arrayOfNulls<Any?>(3)
        a[0] = 0
        a[1] = 1
        a[2] = 2
        return a
    }
    public fun <E> toArray(array: Array<E>): Array<E> {
        val asIntArray = array as Array<Int>
        asIntArray[0] = 0
        asIntArray[1] = 1
        asIntArray[2] = 2
        return array
    }
}

fun box(): String {
    val collection = MyCollection(Arrays.asList(2, 3, 9)) as java.util.Collection<*>

    val array1 = collection.toArray()
    val array2 = collection.toArray(arrayOfNulls<Int>(3) as Array<Int>)

    if (!array1.isArrayOf<Any>()) return (array1 as Object).getClass().toString()
    if (!array2.isArrayOf<Int>()) return (array2 as Object).getClass().toString()

    val s1 = Arrays.toString(array1)
    val s2 = Arrays.toString(array2)

    if (s1 != "[0, 1, 2]") return "s1 = $s1"
    if (s2 != "[0, 1, 2]") return "s2 = $s2"

    return "OK"
}
