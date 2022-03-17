// FIR_IDENTICAL
val flag = true

val a = b@ {
    if (flag) return@b <!RETURN_TYPE_MISMATCH!>4<!>
    return@b
}