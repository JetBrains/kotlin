import kotlin.*
import kotlin.collections.*

@CompileTimeCalculation
fun classCastWithException(a: Any): String {
    return try {
        a as Int
        "Given value is $a and its doubled value is ${2 * a}"
    } catch (e: ClassCastException) {
        "Given value isnt't Int; Exception message: \"${e.message}\""
    }
}

@CompileTimeCalculation
fun safeClassCast(a: Any): Int {
    return (a as? String)?.length ?: -1
}

@CompileTimeCalculation
fun <T> unsafeClassCast(): T {
    return 1 as T
}

@CompileTimeCalculation
fun <T> getIntList() = listOf<Int>(1, 2) as T

@CompileTimeCalculation
fun <T> getStringNullableList() = listOf<String?>(null, "1") as T

@CompileTimeCalculation
fun getLength(str: String) = str.length

@CompileTimeCalculation
class A<T>() {
    fun unsafeCast(): T {
        return 1 as T
    }
}

const val a1 = <!EVALUATED: `Given value is 10 and its doubled value is 20`!>classCastWithException(10)<!>
const val a2 = <!EVALUATED: `Given value isnt't Int; Exception message: "kotlin.String cannot be cast to kotlin.Int"`!>classCastWithException("10")<!>

const val b1 = <!EVALUATED: `-1`!>safeClassCast(10)<!>
const val b2 = <!EVALUATED: `2`!>safeClassCast("10")<!>

// in this example all unsafe cast will be "successful", but will fall when trying to assign
const val c1 = <!EVALUATED: `1`!>unsafeClassCast<Int>()<!>
const val c2 = <!WAS_NOT_EVALUATED: `
Exception java.lang.ClassCastException: kotlin.Int cannot be cast to kotlin.String
	at ClassCastExceptionKt.unsafeClassCast(classCastException.kt:21)
	at ClassCastExceptionKt.<clinit>(classCastException.kt:48)`!>unsafeClassCast<String>()<!>

const val d1 = A<Int>().<!EVALUATED: `1`!>unsafeCast()<!>
const val d2 = A<String>().<!WAS_NOT_EVALUATED: `
Exception java.lang.ClassCastException: kotlin.Int cannot be cast to kotlin.String
	at ClassCastExceptionKt.A.unsafeCast(classCastException.kt:36)
	at ClassCastExceptionKt.<clinit>(classCastException.kt:51)`!>unsafeCast()<!>

const val stringList = getIntList<List<String>>().<!WAS_NOT_EVALUATED: `
Exception java.lang.ClassCastException: kotlin.Int cannot be cast to kotlin.String
	at ClassCastExceptionKt.stringList.<anonymous>(classCastException.kt:54)
	at ClassCastExceptionKt.stringList.Function$0.invoke(classCastException.kt:0)
	at StandardKt.kotlin.let(Standard.kt:32)
	at ClassCastExceptionKt.<clinit>(classCastException.kt:53)`!>let {
    it[0].length
}<!>
const val nullableStringList = getStringNullableList<List<String>>().<!WAS_NOT_EVALUATED: `
Exception java.lang.NullPointerException
	at ClassCastExceptionKt.nullableStringList.<anonymous>(classCastException.kt)
	at ClassCastExceptionKt.nullableStringList.Function$0.invoke(classCastException.kt:0)
	at StandardKt.kotlin.let(Standard.kt:32)
	at ClassCastExceptionKt.<clinit>(classCastException.kt:56)`!>let { it[0].length }<!>
const val nullableStringLength = <!WAS_NOT_EVALUATED: `
Exception java.lang.IllegalArgumentException: Parameter specified as non-null is null: method ClassCastExceptionKt.getLength, parameter str
	at ClassCastExceptionKt.<clinit>(classCastException.kt:31)`!>getLength(getStringNullableList<List<String>>()[0])<!>
