import java.util.ArrayList

class Pair<A, B>(val a: A, val b: B)

class MyListOfPairs<T> : ArrayList<Pair<T, T>>() { }

fun test() {
    MyListOfPairs<Int>() : ArrayList<Pair<Int, Int>>
}
