fun foo(s: String?): Int {
    do {
    } while (s!!.length > 0)
    return s.length
}