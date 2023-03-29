// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY

typealias L<T> = List<T>
typealias NL<T> = List<T>?
typealias LStar<T> = List<*>
typealias MyList<T, X> = List<X>

fun testL1(x: Collection<Any>) = x is L
fun testL2(x: Collection<Int>): List<Int> = x as L
fun testL3(x: Collection<Int>?): List<Int>? = x as L?
fun testL4(x: Collection<Int>?): List<Int>? = x as? L

fun testNL1(x: Collection<Int>?): Boolean = x is NL
fun testNL2(x: Collection<Int>?): List<Int>? = x as NL
fun testNL3(x: Collection<Int>?): List<Int>? = x as NL<!REDUNDANT_NULLABLE!>?<!>

fun testLStar(x: Collection<Int>): List<Int> = x as LStar
fun testMyList(x: Collection<Int>): List<Int> = x as MyList

typealias MMTT<T> = MutableMap<T, T>
typealias Dictionary<T> = MutableMap<String, T>
typealias WriteableMap<K, V> = MutableMap<in K, V>
typealias ReadableList<T> = MutableList<out T>

fun testWrong1(x: Map<Any, Any>) = x is <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>MMTT<!>
fun testWrong2(x: Map<Any, Any>) = x is <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Dictionary<!>
fun testWrong3(x: Map<Any, Any>) = x is <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>WriteableMap<!>
fun testWrong4(x: List<Any>) = x is <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>ReadableList<!>

fun <T> testLocal(x: Any) {
    class C
    typealias CA = C
    if (x is <!CANNOT_CHECK_FOR_ERASED!>C<!>) {}
    if (x is <!CANNOT_CHECK_FOR_ERASED!>CA<!>) {}
}
