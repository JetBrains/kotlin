// !CHECK_TYPE

//KT-2324 Can't resolve generic by type of function result
package i

//+JDK
import java.util.*

fun <T, K> someFunction(list: List<T>, transform: (T) -> K): List<K> {
    val result = arrayList<K>()
    for (i in list) {
        result.add(transform(i))
    }
    return result
}

fun testSomeFunction() {
    val result1 = someFunction(arrayList<Int>(1, 2), {checkSubtype<Int>(it)}) //type of result1 is List<Int>
    assertEquals(1, result1.get(0)); //OK

    val result2 = someFunction(arrayList<Int>(1, 2), {it}) // type of result2 is List<DONT_CARE>
    checkSubtype<List<Int>>(result2)
    assertEquals(1, result2.get(0)); //resolved to error element
}

//---------------------------------
fun assertEquals(<!UNUSED_PARAMETER!>expected<!>: Any?, <!UNUSED_PARAMETER!>actual<!>: Any?, <!UNUSED_PARAMETER!>message<!>: String = "") {
}

fun <T> arrayList(vararg values: T) : ArrayList<T> = values.toCollection(ArrayList<T>(values.size))

fun <T, C: MutableCollection<in T>> Array<T>.toCollection(result: C) : C {
    for (element in this) result.add(element)
    return result
}
