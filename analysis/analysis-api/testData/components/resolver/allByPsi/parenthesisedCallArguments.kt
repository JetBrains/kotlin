fun foo(x: Int) {}
fun fooVararg(vararg x: Int) {}

fun fooLambda(lambda: (x: Int) -> Int) {}

fun interface MySam {
    fun foo(): Int
}

fun fooSam(sam: MySam) {}

fun main() {
    var value = 1

    foo(3)
    foo(((5)))
    foo(x = ((5)))
    foo((((5 as Int))))
    foo(x = (((5 as Int))))

    foo((((when (((value))) {
        1 -> 1
        else -> 2
    }))))

    foo(((((++value)))))

    fooVararg((5), (5 as Int))
    fooVararg(*(intArrayOf(5)))
    fooVararg(x = (intArrayOf(5)))

    fooLambda((({ _ -> 5})))

    fooSam((((MySam { 5 }))))
}