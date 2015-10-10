// !CHECK_TYPE

package h
//+JDK
import java.util.*

fun <T> id(t: T) : T = t

fun <T> id1(t: T) = t

fun <R> elem(t: List<R>): R = t.get(0)

fun <R> elemAndList(<!UNUSED_PARAMETER!>r<!>: R, t: List<R>): R = t.get(0)

fun <T> both(t1: T, <!UNUSED_PARAMETER!>t2<!>: T) : T = t1

fun test1() {
    val a = elem(list(2))
    val b = id(elem(list(2)))
    val c = id(id1(id(id1(list(33)))))
    checkSubtype<Int>(a)
    checkSubtype<Int>(b)
    checkSubtype<List<Int>>(c)

    val d : ArrayList<Int> = newList()
    val e : ArrayList<Int> = id(newList())
    val f : ArrayList<Int> = id(id1(id(id1(newList()))))

    checkSubtype<List<Int>>(d)
    checkSubtype<List<Int>>(e)
    checkSubtype<List<Int>>(f)

    val g = elemAndList("", newList())
    val h = elemAndList<Long>(1, newList<Long>())

    checkSubtype<String>(g)
    checkSubtype<Long>(h)

    val i = both(1, "")
    val j = both(id(1), id(""))
    checkSubtype<Any>(i)
    checkSubtype<Any>(j)
}

fun <T> list(value: T) : ArrayList<T> {
    val list = ArrayList<T>()
    list.add(value)
    return list
}

fun <S> newList() : ArrayList<S> {
    return ArrayList<S>()
}
