fun foo(s: String?) contract [bar(s)] {
    s ?: throw NullArgumentException()
}