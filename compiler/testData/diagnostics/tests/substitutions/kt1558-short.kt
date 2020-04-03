// FIR_IDENTICAL
fun <E> List<*>.toArray(ar: Array<E>): Array<E> = ar

fun testArrays(ci : List<Int>) {
    ci.toArray<Int>(<!UNRESOLVED_REFERENCE!>x<!>)
}