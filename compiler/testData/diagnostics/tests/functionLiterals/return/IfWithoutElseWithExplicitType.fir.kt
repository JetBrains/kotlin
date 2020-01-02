val flag = true

val a: () -> Int = l@ {
    if (flag) return@l 4
}

val b: () -> Unit = l@ {
    if (flag) return@l 4
}

val c: () -> Any = l@ {
    if (flag) return@l 4
}

val d: () -> Int = l@ {
    if (flag) return@l 4
    5
}

val e: () -> Int = l@ {
    if (flag) 4
}