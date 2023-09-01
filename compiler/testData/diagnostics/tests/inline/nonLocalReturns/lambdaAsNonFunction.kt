// FIR_IDENTICAL
fun box() : String {
    test {
        <!RETURN_NOT_ALLOWED!>return@box<!> "123"
    }

    return "OK"
}

<!NOTHING_TO_INLINE!>inline<!> fun test(p: Any) {
    p.toString()
}