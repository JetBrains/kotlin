package a

import java.util.ArrayList

public fun <T> Iterable<T>.withIndices(): List<Pair<Int, T>> {
    val answer = ArrayList<Pair<Int, T>>()
    var nextIndex = 1
    for (e in this) {
        answer.add(Pair(nextIndex, e))
        nextIndex++
    }
    return answer
}

//from standard library
public class Pair<out A, out B>(
    public val first: A,
    public val second: B
)