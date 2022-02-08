
fun box() : String {
    test {
        return@box "123"
    }

    return "OK"
}

<!NOTHING_TO_INLINE!>inline<!> fun <T> test(p: T) {
    p.toString()
}
