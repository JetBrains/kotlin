// !LANGUAGE: +ProperIeee754Comparisons

fun test(x: Any?): String {
    if (x !is Int) return "Fail 1"
    when (x) {
        0 -> return "OK"
        else -> return "Fail 2"
    }
}

fun box(): String = test(0)