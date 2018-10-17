fun test(s: String?): Int {
    if (s != null) {
        return@test<caret> 1
    }
    return 0
}