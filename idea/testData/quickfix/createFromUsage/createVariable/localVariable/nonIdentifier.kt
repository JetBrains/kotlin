// "Create local variable '-'" "false"
// ACTION: Create extension function 'A.minus'
// ACTION: Create member function 'A.minus'
// ACTION: Replace overloaded operator with function call
// ERROR: Unresolved reference. None of the following candidates is applicable because of receiver type mismatch: <br>public inline operator fun BigDecimal.minus(other: BigDecimal): BigDecimal defined in kotlin<br>public inline operator fun BigInteger.minus(other: BigInteger): BigInteger defined in kotlin<br>public operator fun <T> Iterable<???>.minus(elements: Array<out ???>): List<???> defined in kotlin.collections<br>public operator fun <T> Iterable<???>.minus(elements: Iterable<???>): List<???> defined in kotlin.collections<br>public operator fun <T> Iterable<???>.minus(elements: Sequence<???>): List<???> defined in kotlin.collections<br>public operator fun <T> Iterable<A>.minus(element: A): List<A> defined in kotlin.collections<br>public operator fun <K, V> Map<out ???, ???>.minus(keys: Array<out ???>): Map<???, ???> defined in kotlin.collections<br>public operator fun <K, V> Map<out ???, ???>.minus(keys: Iterable<???>): Map<???, ???> defined in kotlin.collections<br>public operator fun <K, V> Map<out ???, ???>.minus(keys: Sequence<???>): Map<???, ???> defined in kotlin.collections<br>public operator fun <K, V> Map<out A, ???>.minus(key: A): Map<A, ???> defined in kotlin.collections<br>public operator fun <T> Set<???>.minus(elements: Array<out ???>): Set<???> defined in kotlin.collections<br>public operator fun <T> Set<???>.minus(elements: Iterable<???>): Set<???> defined in kotlin.collections<br>public operator fun <T> Set<???>.minus(elements: Sequence<???>): Set<???> defined in kotlin.collections<br>public operator fun <T> Set<A>.minus(element: A): Set<A> defined in kotlin.collections<br>public operator fun <T> Sequence<???>.minus(elements: Array<out ???>): Sequence<???> defined in kotlin.sequences<br>public operator fun <T> Sequence<???>.minus(elements: Iterable<???>): Sequence<???> defined in kotlin.sequences<br>public operator fun <T> Sequence<???>.minus(elements: Sequence<???>): Sequence<???> defined in kotlin.sequences<br>public operator fun <T> Sequence<A>.minus(element: A): Sequence<A> defined in kotlin.sequences
// COMPILER_ARGUMENTS: -XXLanguage:-NewInference
class A

fun bar() {
    val a = A()
    return a <caret>- a
}
