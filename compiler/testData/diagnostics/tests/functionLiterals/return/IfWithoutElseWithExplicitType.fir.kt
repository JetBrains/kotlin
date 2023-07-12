val flag = true

val a: () -> Int = <!INITIALIZER_TYPE_MISMATCH!>l@ {
    if (flag) return@l 4
}<!>

val b: () -> Unit = <!INITIALIZER_TYPE_MISMATCH!>l@ {
    if (flag) return@l 4
}<!>

val c: () -> Any = l@ {
    if (flag) return@l 4
}

val d: () -> Int = l@ {
    if (flag) return@l 4
    5
}

val e: () -> Int = <!INITIALIZER_TYPE_MISMATCH!>l@ {
    if (flag) 4
}<!>
