package c

interface A<T>

fun test(a: A<Int>?) {
    a.<!INAPPLICABLE_CANDIDATE!>foo<!>() //no error
}

fun <R> A<R>.foo() {}

//------------
fun test(nullabilityInfoMap: Map<Int, Any>?) {
    nullabilityInfoMap.<!INAPPLICABLE_CANDIDATE!>iterator<!>() //no error
}

//resolves to
public fun <K,V> Map<K,V>.iterator(): Iterator<Map.Entry<K,V>> {}


//-------------
fun foo() : Boolean {
    val nullableList = getNullableList()
    return nullableList.<!INAPPLICABLE_CANDIDATE!>contains<!>("")

}

fun getNullableList(): List<String>? = null