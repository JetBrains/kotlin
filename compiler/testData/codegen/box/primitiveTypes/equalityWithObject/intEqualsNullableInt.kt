// !LANGUAGE: +ProperIeee754Comparisons

fun eq1(a: Int?, b: Int) = a == b

fun eq2(a: Int, b: Int?) = a == b

fun eq3(a: Int?, b: Int?) = a == b

fun box(): String =
    when {
        !eq1(1, 1) -> "Fail 1"
        !eq2(1, 1) -> "Fail 2"
        !eq3(1, 1) -> "Fail 3"
        else -> "OK"
    }