import kotlin.coroutines.experimental.*

fun main(args: Array<String>) {
    val sq = buildSequence {
        for (i in 0..6 step 2) {
            print("before: $i ")
            yield(i)
            println("after: $i")
        }
    }
    println("Got: ${sq.joinToString(separator = " ")}")
}