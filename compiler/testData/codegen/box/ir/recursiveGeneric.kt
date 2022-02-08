
open class B<T>

open class BB<T>

interface I<T>
interface II<T> {
    fun bar(): String = "OK"
}

class CC : I<CC>

inline fun <T : I<T>> test0(a: Any): String = (a as? T != null).toString()[0].toString()

class CCC : I<CCC>, II<CCC>, B<CCC>()

inline fun <T> test1(a: Any): String where
        T : I<T>,
        T : II<T>,
        T : B<T> {
            return (a as? T != null).toString()[1].toString()
        }

class CCCC : I<CCCC>, II<CCCC>

inline fun <T> test2(a: Any): String where
        T : I<T>,
        T : II<T> {
    return (a as? T != null).toString()[2].toString()
}

open class BX<T1, T2>
class CI : I<CI>
class DB : BX<DB, CI>()
class CIB : BX<CIB, CI>(), I<CIB>

inline fun <TI: I<TI>, TC : BX<TC, TI>> test3(a: Any): String {
    val s1 = (a as? TC != null).toString()[3].toString()
    val s2 = (a as? TI != null).toString()[3].toString()
    return return s1 + s2
}

// KT-47342

object O : II<I<Any>>

inline fun <T : I<T>> testKt(): II<T> = O as II<T>

fun foo(a: Any): String = a.toString()

fun box(): String {
    var result = ""

    result += foo(test0<CC>(Any()))
    result += foo(test0<CC>(CC()))

    result += foo(test1<CCC>(Any()))
    result += foo(test1<CCC>(CCC()))

    result += foo(test2<CCCC>(Any()))
    result += foo(test2<CCCC>(CCCC()))

    result += foo(test3<CI, DB>(Any()))
    result += foo(test3<CI, DB>(CI()))
    result += foo(test3<CI, DB>(DB()))
    result += foo(test3<CI, DB>(CIB()))

    if (result != "ftarlussseesee") return "FAIL: $result"

    return foo(testKt<CC>().bar())
}