fun naturalOrder<in T : Comparable<T>>(a : T, b : T) : Int = a.compareTo(b)

fun castingNaturalOrder(a : Object, b : Object) : Int = (a as Comparable<Object>).compareTo(b as Comparable<Object>)

enum class ComparisonResult {
    LS, EQ, GR;
}

fun <T> asMatchableComparison(cmp : Comparison<T>) : MatchableComparison<T> = {a, b ->

    if (val res = cmp(a, b); res == 0) return ComparisonResult.EQ
    else if (res < 0) return ComparisonResult.LS
    return ComparisonResult.GR
}

