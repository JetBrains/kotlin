// !DIAGNOSTICS: -UNUSED_PARAMETER

class C {
    operator fun compareTo(c: C): Int? = null
}

fun test(c: C) {
    c < c
    c <= c
    c >= c
    c > c
}