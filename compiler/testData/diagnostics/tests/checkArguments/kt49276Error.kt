// WITH_STDLIB
// !LANGUAGE: +ProgressionsChangingResolve -DisableCheckingChangedProgressionsResolve
// This test is not K1/K2 identical due to KT-58789 not implemented yet

fun <E> SmartList(x: E) {}
fun <E> SmartList(x: Collection<E>) {}

fun append(x: Any?) {}
fun append(x: Collection<*>) {}

fun append2(x: Iterable<*>) {}
fun append2(x: Collection<*>) {}

class In<in T>(x: T)

@JvmName("append31")
fun append3(x: In<Nothing>) {}
fun append3(x: In<Collection<*>>) {}

fun <E> append4(x: E) {}
fun <E: Collection<*>> append4(x: E) {}

fun main() {
    SmartList(<!PROGRESSIONS_CHANGING_RESOLVE_ERROR("fun <E> SmartList(x: Collection<E>): Unit")!>1..2<!>) // warning
    SmartList<IntRange>(1..10) // no warning

    append(<!PROGRESSIONS_CHANGING_RESOLVE_ERROR("fun append(x: Collection<*>): Unit")!>1..10<!>)    // warning
    append((1..10) as Any) // no warning
    append((1..10) as Iterable<Int>) // no warning
    append("a".."z") // no warning, the range is not iterable
    append(1.0..2.0)

    append2(1..10)    // no warning

    append3(In(1..10))    // no warning

    append4(<!PROGRESSIONS_CHANGING_RESOLVE_ERROR("fun <E : Collection<*>> append4(x: E): Unit")!>1..10<!>)    // warning
}
