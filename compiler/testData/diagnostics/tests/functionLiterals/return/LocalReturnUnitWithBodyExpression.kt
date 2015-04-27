val flag = true

// type of lambda was checked by txt
val a = b@ { // () -> Unit
    if (flag) return@b
    else <!UNUSED_EXPRESSION!>54<!>
}