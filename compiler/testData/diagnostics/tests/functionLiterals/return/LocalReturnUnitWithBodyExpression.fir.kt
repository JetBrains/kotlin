// !DIAGNOSTICS: +UNUSED_EXPRESSION

val flag = true

// type of lambda was checked by txt
val a = b@ { // () -> Unit
    if (flag) return@b
    else 54
}
