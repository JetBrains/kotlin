fun foo(s: String?) {
    while (true) {
        val t = s?.hashCode() ?:<caret> continue
    }
}