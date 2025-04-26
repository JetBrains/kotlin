// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ForbidInferOfInvisibleTypeAsReifiedVarargOrReturnType
// DONT_WARN_ON_ERROR_SUPPRESSION
// WITH_STDLIB

// FILE: test1/source.kt
package test1

private interface PrivateInFileInterface

data object Public1 : PrivateInFileInterface
data object Public2 : PrivateInFileInterface

// FILE: test2/source.kt
package test2

import test1.Public1
import test1.Public2

fun <T> f1(x: T, y: T): Unit {
    mutableListOf(x, y)
}

fun <T> f2(vararg x: T) = Unit
@Suppress("MULTIPLE_VARARG_PARAMETERS")
fun <T> f2_(vararg x: T, vararg y: T) = Unit
inline fun <reified T> f3(x: T, y: T): Unit {
    listOf(x, y)
}

inline fun <reified T> f4(x: T, y: T): Unit {
    listOf(x) + listOf(y)
}

inline fun <reified T> f5(x: T, y: T): Unit {
    arrayOfNulls<T>(0)
}

inline fun <reified T> f6(x: T, y: T): Unit {
    emptyArray<T>()
}
<!NOTHING_TO_INLINE!>inline<!> fun <T> f7(x: Array<T>): Unit {
    x
}

fun <T> f8(x: T, y: T): T = x
fun <T> f9(x: T, y: T): List<T> = listOf(x)

object `_`

fun main() {
    println(f1<_>(Public1, Public2)) // ok
    println(f1(Public1, Public2)) // ok
    println(f2<<!INFERRED_INVISIBLE_VARARG_TYPE_ARGUMENT_ERROR!>_<!>>(Public1, Public2)) // fails in run time
    println(<!INFERRED_INVISIBLE_VARARG_TYPE_ARGUMENT_ERROR!>f2<!>(Public1, Public2)) // fails in run time
    println(f2_(Public1, Public2)) // fails in compile time
    println(f3<<!INFERRED_INVISIBLE_REIFIED_TYPE_ARGUMENT_ERROR!>_<!>>(Public1, Public2)) // fails in run time
    println(<!INFERRED_INVISIBLE_REIFIED_TYPE_ARGUMENT_ERROR!>f3<!>(Public1, Public2)) // fails in run time
    println(<!INFERRED_INVISIBLE_REIFIED_TYPE_ARGUMENT_ERROR!>f4<!>(Public1, Public2)) // ok
    println(f4<<!INFERRED_INVISIBLE_REIFIED_TYPE_ARGUMENT_ERROR!>_<!>>(Public1, Public2)) // ok
    println(<!INFERRED_INVISIBLE_REIFIED_TYPE_ARGUMENT_ERROR!>f5<!>(Public1, Public2)) // fails in run time
    println(f5<<!INFERRED_INVISIBLE_REIFIED_TYPE_ARGUMENT_ERROR!>_<!>>(Public1, Public2)) // fails in run time
    println(<!INFERRED_INVISIBLE_REIFIED_TYPE_ARGUMENT_ERROR!>f6<!>(Public1, Public2)) // fails in run time
    println(f6<<!INFERRED_INVISIBLE_REIFIED_TYPE_ARGUMENT_ERROR!>_<!>>(Public1, Public2)) // fails in run time
    println(f7(<!INFERRED_INVISIBLE_REIFIED_TYPE_ARGUMENT_ERROR!>arrayOf<!>(Public1, Public2))) // fails in run time
    println(f7<_>(<!INFERRED_INVISIBLE_REIFIED_TYPE_ARGUMENT_ERROR!>arrayOf<!>(Public1, Public2))) // fails in run time
    val f8_1 = <!INFERRED_INVISIBLE_RETURN_TYPE_ERROR!>f8(Public1, Public2)<!> // fails in run time
    val f8_2 = <!INFERRED_INVISIBLE_RETURN_TYPE_ERROR!>f8<_>(Public1, Public2)<!> // fails in run time
    val f9_1 = <!INFERRED_INVISIBLE_RETURN_TYPE_ERROR!>f9(Public1, Public2)<!> // fails in run time
    val f9_2 = <!INFERRED_INVISIBLE_RETURN_TYPE_ERROR!>f9<_>(Public1, Public2)<!> // fails in run time
    f8(<!UNRESOLVED_REFERENCE!>Unresolved<!>(), 2)
    <!INFERRED_INVISIBLE_RETURN_TYPE_ERROR!>f8(listOf(Public1), listOf(Public2))<!>[0] // fails in run time
    val condition1 = <!INFERRED_INVISIBLE_WHEN_TYPE_ERROR!>if (2.toLong() == 3L) Public1 else Public2<!> // fails in run time
    <!INFERRED_INVISIBLE_WHEN_TYPE_ERROR!>if (2.toLong() == 3L) Public1 else Public2<!> // fails in run time
    if (2.toLong() == 3L) Public1 else Public2.let {}
    val condition2 = <!INFERRED_INVISIBLE_WHEN_TYPE_ERROR!>if (2.toLong() == 3L) listOf(Public1) else listOf(Public2)<!> // fails in run time
    <!INFERRED_INVISIBLE_WHEN_TYPE_ERROR!>if (2.toLong() == 3L) listOf(Public1) else listOf(Public2)<!> // fails in run time
    if (2.toLong() == 3L) listOf(Public1) else listOf(Public2).let {}
    <!INFERRED_INVISIBLE_WHEN_TYPE_ERROR!>when { // fails in run time
        2.toLong() == 3L -> listOf(Public1)
        else -> listOf(Public2)
    }<!>
    val condition3 = <!INFERRED_INVISIBLE_WHEN_TYPE_ERROR!>when { // fails in run time
        2.toLong() == 3L -> listOf(Public1)
        else -> listOf(Public2)
    }<!>
    when {
        2.toLong() == 3L -> listOf(Public1)
        else -> listOf(Public2).let {}
    }
    <!INFERRED_INVISIBLE_WHEN_TYPE_ERROR!>when (2.toLong()) { // fails in run time
        3L -> listOf(Public1)
        else -> listOf(Public2)
    }<!>
    val condition4 = <!INFERRED_INVISIBLE_WHEN_TYPE_ERROR!>when (2.toLong()) { // fails in run time
        3L -> listOf(Public1)
        else -> listOf(Public2)
    }<!>
    when (2.toLong()) {
        3L -> listOf(Public1)
        else -> listOf(Public2).let {}
    }

    println(f2<test1.<!INVISIBLE_REFERENCE!>PrivateInFileInterface<!>>(Public1, Public2))
    println(f3<test1.<!INVISIBLE_REFERENCE!>PrivateInFileInterface<!>>(Public1, Public2))
    println(f3<`_`>(`_`, `_`))
}
