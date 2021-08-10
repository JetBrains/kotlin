// FULL_JDK
fun collapse(path: String) {
    val result = (path as java.lang.String).replace("123", "456")
    if (<!EQUALITY_NOT_APPLICABLE_WARNING!>result !== path<!>) {}
}