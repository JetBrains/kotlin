// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_EXPRESSION

fun <D> makeDefinitelyNotNull(arg: D?): D = TODO()

fun <N : Number?> test(arg: N) {
    makeDefinitelyNotNull(arg) ?: 1

    makeDefinitelyNotNull(arg)!!

    makeDefinitelyNotNull(arg)?.toInt()

    val nullImposible = when (val dnn = makeDefinitelyNotNull(arg)) {
        null -> false
        else -> true
    }
}