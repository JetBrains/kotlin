// FIR_IDENTICAL
// NI_EXPECTED_FILE
val flag = true

// type of a was checked by txt
val a = run { // () -> Unit
    return@run
}

// Unit
val b = run {
    if (flag) return@run
    5
}

// Unit
val c = run {
    if (flag) return@run

    return@run <!RETURN_TYPE_MISMATCH!>4<!>
}
