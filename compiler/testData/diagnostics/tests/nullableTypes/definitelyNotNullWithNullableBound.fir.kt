// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_EXPRESSION

fun <D> makeDefinitelyNotNull(arg: D?): D = TODO()

fun <N : Number?> test(arg: N) {
    makeDefinitelyNotNull(arg) <!USELESS_ELVIS!>?: 1<!>

    makeDefinitelyNotNull(arg)<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>

    makeDefinitelyNotNull(arg)<!UNNECESSARY_SAFE_CALL!>?.<!>toInt()

    val nullImposible = when (val dnn = makeDefinitelyNotNull(arg)) {
        null -> false
        else -> true
    }
}
