package n

//+JDK
import java.util.*

fun <T> expected(t: T, <!UNUSED_PARAMETER!>f<!>: () -> T) : T = t

fun test(arrayList: ArrayList<Int>, list: List<Int>) {
    val <!UNUSED_VARIABLE!>t<!> = expected(arrayList, { list.reverse() })
}

fun <T> List<T>.reverse() : List<T> = this