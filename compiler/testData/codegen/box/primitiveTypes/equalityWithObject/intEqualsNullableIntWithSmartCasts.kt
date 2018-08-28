// !LANGUAGE: +ProperIeee754Comparisons

fun eq1(a: Any?, b: Any?) =
    a is Int? && b is Int && a == b

fun eq2(a: Any?, b: Any?) =
    a is Int && b is Int? && a == b

fun eq3(a: Any?, b: Any?) =
    a is Int && b is Int && a == b

fun eq4(a: Any?, b: Any?) =
    a is Int? && b is Int? && a == b

fun box(): String =
    when {
        !eq1(1, 1) -> "Fail 1"
        !eq2(1, 1) -> "Fail 2"
        !eq3(1, 1) -> "Fail 3"
        !eq4(1, 1) -> "Fail 4"
        else -> "OK"
    }