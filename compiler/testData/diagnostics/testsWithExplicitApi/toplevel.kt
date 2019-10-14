<!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>fun foo()<!> {}

public fun foo2() {}

<!NO_EXPLICIT_RETURN_TYPE_IN_API_MODE, NO_EXPLICIT_VISIBILITY_IN_API_MODE!>fun bar()<!> = 10
<!NO_EXPLICIT_RETURN_TYPE_IN_API_MODE!>public fun bar2()<!> = 10
public fun bar3(): Int = 10
