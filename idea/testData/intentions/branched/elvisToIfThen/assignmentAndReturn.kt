fun foo(s: String?) {
    val t = s?.hashCode() ?:<caret> return
}