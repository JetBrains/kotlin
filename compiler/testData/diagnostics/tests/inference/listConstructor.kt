package a
//+JDK
import java.util.*

fun <A> cons(<!UNUSED_PARAMETER!>x<!>: A, xs: List<A>): List<A> = xs

fun <B> nil(): List<B> = arrayList()

fun test() {
    val xs = cons(1, nil())
    val xs1 = cons("", nil())
    val xs2 = cons(1, nil<Any>())

    xs : List<Int>
    xs1 : List<String>
    xs2 : List<Any>
}


// ---------------------
// copy from kotlin util

fun arrayList<T>(vararg values: T) : ArrayList<T> = values.toCollection(ArrayList<T>(values.size()))

fun <T, C: MutableCollection<in T>> Array<T>.toCollection(result: C) : C {
    for (element in this) result.add(element)
    return result
}
