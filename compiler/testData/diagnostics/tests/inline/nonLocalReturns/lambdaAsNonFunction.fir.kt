fun box() : String {
    test {
        return@box "123"
    }

    return "OK"
}

inline fun test(p: Any) {
    p.toString()
}