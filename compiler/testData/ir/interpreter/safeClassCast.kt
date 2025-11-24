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
