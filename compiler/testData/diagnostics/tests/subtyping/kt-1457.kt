// !CHECK_TYPE

import java.util.ArrayList

class Pair<A, B>(val a: A, val b: B)

class MyListOfPairs<T> : ArrayList<Pair<T, T>>() { }

fun test() {
    checkSubtype<ArrayList<Pair<Int, Int>>>(MyListOfPairs<Int>())
}
