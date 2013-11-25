tailRecursive fun <T, A> Iterator<T>.foldl(acc : A, foldFunction : (e : T, acc : A) -> A) : A =
        if (!hasNext()) acc
        else foldl(foldFunction(next(), acc), foldFunction)

fun box() : String {
    val sum = (1..1000000).iterator().foldl(0) { (e : Int, acc : Long) ->
        acc + e
    }

    return if (sum == 500000500000) "OK" else "FAIL: $sum"
}