// REPL_MODE: ~REPL_MODE~

val a = 1
when(a) {
    1 -> println("1")
    else -> println("2")
}

when(a) {
    1 -> println("1")
}

when(a) {
    2 -> println("2")
    else -> println("1")
}

when(a) {
    1 -> 11
    else -> 12
}