package util

fun foo(): Int {
    val prop = "str"

    @ForAnno("for anno $prop")
    for (@ForParameter("for $prop") i in 1..100) {}
    for (@ForParameter("second for $prop") (x, @NestedParam("destructuring in for $prop") y) in bar()) {}
    withLambda { (@LeftLambda("lambda a $prop") a, @RightLambda("lambda b $prop") b) ->

    }

    @Destructuring("destr $prop")
    val (@LeftDestructuring("a $prop") a, @RightDestructuring("b $prop") b) = Pair(0, 1)
    return a + b
}
