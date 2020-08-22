package sample

fun test() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>arrayListOf<!>(1, 2, 3)
}