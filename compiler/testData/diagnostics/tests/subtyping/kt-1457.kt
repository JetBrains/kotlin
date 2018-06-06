// !CHECK_TYPE
// JAVAC_EXPECTED_FILE

import java.util.ArrayList

class Pair<A, B>(val a: A, val b: B)

class MyListOfPairs<T> : ArrayList<Pair<T, T>>() { }

fun test() {
    checkSubtype<ArrayList<Pair<Int, Int>>>(MyListOfPairs<Int>())
}
