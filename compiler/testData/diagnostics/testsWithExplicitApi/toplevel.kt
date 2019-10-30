// SKIP_TXT

<!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>fun foo<!>() {}

public fun foo2() {}

<!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>fun <!NO_EXPLICIT_RETURN_TYPE_IN_API_MODE!>bar<!><!>() = 10
public fun <!NO_EXPLICIT_RETURN_TYPE_IN_API_MODE!>bar2<!>() = 10
public fun bar3(): Int = 10
