// TARGET_BACKEND: JVM
// WITH_STDLIB
// JVM_ABI_K1_K2_DIFF: KT-63828

class MyCollection<T>(val delegate: Collection<T>): Collection<T> by delegate

fun box(): String {
    val collection = MyCollection(listOf(2, 3, 9)) as java.util.Collection<*>

    val array1 = collection.toArray()
    val array2 = collection.toArray(arrayOfNulls<Int>(3) as Array<Int>)

    if (!array1.isArrayOf<Any>()) return (array1 as Object).getClass().toString()
    if (!array2.isArrayOf<Int>()) return (array2 as Object).getClass().toString()

    val s1 = array1.contentToString()
    val s2 = array2.contentToString()

    if (s1 != "[2, 3, 9]") return "s1 = $s1"
    if (s2 != "[2, 3, 9]") return "s2 = $s2"

    return "OK"
}
