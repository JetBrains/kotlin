fun foo(s: String?) contract [returns() implies (s != null)] {
    s ?: throw NullArgumentException()
}

// COMPILATION_ERRORS
