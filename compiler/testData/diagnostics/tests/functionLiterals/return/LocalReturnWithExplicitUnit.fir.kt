val flag = true

// type of lambda was checked by txt
val a = l@ { // () -> Any
    if (flag) return@l 4
    return@l Unit
}

val b = l@ { // () -> Any
    if (flag) return@l Unit
    5
}

val c = l@ { // () -> Unit
    if (flag) return@l Unit
}