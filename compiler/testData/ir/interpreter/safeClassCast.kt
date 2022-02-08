import kotlin.collections.*

@CompileTimeCalculation
fun <T> foo(): String {
    return if (listOf<Int>() as? T == null) "Can't cast" else "Safe cast"
}

@CompileTimeCalculation
inline fun <reified T> bar(): String {
    return if (listOf<Int>() as? T == null) "Can't cast" else "Safe cast"
}

inline fun <reified T> arrayCast(vararg t: T): Array<T> = t as Array<T>

const val a1 = <!EVALUATED: `Safe cast`!>foo<Int>()<!>
const val a2 = <!EVALUATED: `Safe cast`!>foo<Int?>()<!>
const val a3 = <!EVALUATED: `Safe cast`!>foo<Double?>()<!>
const val a4 = <!EVALUATED: `Safe cast`!>foo<List<*>>()<!>
const val a5 = <!EVALUATED: `Safe cast`!>foo<Map<*,*>>()<!>

const val b1 = <!EVALUATED: `Can't cast`!>bar<Int>()<!>
const val b2 = <!EVALUATED: `Can't cast`!>bar<Int?>()<!>
const val b3 = <!EVALUATED: `Can't cast`!>bar<Double?>()<!>
const val b4 = <!EVALUATED: `Can't cast`!>bar<Map<*,*>>()<!>
const val b5 = <!EVALUATED: `Safe cast`!>bar<List<Int>>()<!>
const val b6 = <!EVALUATED: `Safe cast`!>bar<List<String>>()<!>

const val c1 = <!EVALUATED: `true`!>arrayOf<Int>(1, 2, 3) as? Array<String> == null<!>
const val c2 = <!EVALUATED: `false`!>arrayOf<Int>(1, 2, 3) as? Array<Number> == null<!>
const val c3 = <!EVALUATED: `true`!>arrayOf<Any>(listOf(1, 2), listOf(2, 3)) as? Array<List<String>?> == null<!>
const val c4 = <!EVALUATED: `false`!>arrayOf<List<Int>>(listOf(1, 2), listOf(2, 3)) as? Array<List<String>?> == null<!>
const val c5 = <!EVALUATED: `true`!>arrayOf<List<Int>>(listOf(1, 2), listOf(2, 3)) as? Array<Set<Int>> == null<!>
const val c6 = <!EVALUATED: `false`!>arrayOf<List<Int>>(listOf(1, 2), listOf(2, 3)) as? Array<Collection<String>> == null<!>
const val c7 = <!EVALUATED: `false`!>Array<List<Int>>(3) { listOf(it, it + 1) } as? Array<List<String>?> == null<!>
const val c8 = <!EVALUATED: `true`!>Array<List<Int>>(3) { listOf(it, it + 1) } as? Array<Set<Int>> == null<!>

const val d1 = arrayCast(arrayOf<Int>(1, 2, 3)).<!EVALUATED: `1`!>size<!>
const val d2 = arrayCast(*arrayOf<Int>(1, 2, 3)).<!EVALUATED: `3`!>size<!>
const val d3 = arrayCast<Int>(1, 2, 3).<!EVALUATED: `3`!>size<!>
