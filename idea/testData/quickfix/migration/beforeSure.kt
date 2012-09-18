// "Replace sure() calls by !! in project" "true"

package p

fun println(a: Any) = a

fun int(): Int? = 1

fun test1(a: Int?) {
    println(a.<caret>sure() + 1)
    println(a.sure<Int>() + 1)
    println((a).sure<Int>() + 1)
    println(int().sure<Int>() + 1)
    println(p.int().sure<Int>() + 1)
    println(if (true) {
        null
    } else {
        2
    }.sure<Int>() + 1)
    println(((2 + 1): Int?).sure<Int>() + 1)
    sure()
    p.sure()
}

fun sure() {}


class A {

    fun Int?.sure(x : Int) = x
    fun String?.sure<T, R>() {}

    fun test(x: Int?) {
        x.sure(1)
        "".sure<Int, Int>()
    }
}

