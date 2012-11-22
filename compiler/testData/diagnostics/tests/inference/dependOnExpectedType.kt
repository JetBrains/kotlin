package h
//+JDK
import java.util.*

fun <T> id(t: T) : T = t

fun <T> id1(t: T) = t

fun <R> elem(t: List<R>): R = t.get(0)

fun <R> elemAndList(<!UNUSED_PARAMETER!>r<!>: R, t: List<R>): R = t.get(0)

fun both<T>(t1: T, <!UNUSED_PARAMETER!>t2<!>: T) : T = t1

fun test1() {
    val a = elem(list(2))
    val b = id(elem(list(2)))
    val c = id(id1(id(id1(list(33)))))
    a : Int
    b : Int
    c : List<Int>

    val d : ArrayList<Int> = newList()
    val e : ArrayList<Int> = id(newList())
    val f : ArrayList<Int> = id(id1(id(id1(newList()))))

    d : List<Int>
    e : List<Int>
    f : List<Int>

    val g = elemAndList("", newList())
    val h = elemAndList<Long>(1, newList<Long>())

    g : String
    h : Long

    val i = both(1, "")
    val j = both(id(1), id(""))
    i : Comparable<*>
    j : Comparable<*>
}

fun list<T>(value: T) : ArrayList<T> {
    val list = ArrayList<T>()
    list.add(value)
    return list
}

fun newList<S>() : ArrayList<S> {
    return ArrayList<S>()
}
