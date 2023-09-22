package extensionFunctions

<!CONFLICTING_OVERLOADS!>fun Int.qwe(a: Float)<!> = 1

<!CONFLICTING_OVERLOADS!>@Deprecated("qwe", level = DeprecationLevel.HIDDEN)
fun Int.qwe(a: Float)<!> = 2
