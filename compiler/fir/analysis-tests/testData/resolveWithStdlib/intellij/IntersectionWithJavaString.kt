// FULL_JDK
// ISSUE: KT-48113
// WITH_EXTENDED_CHECKERS

fun collapse(path: String) {
    val result = (path as <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.lang.String<!>).replace("123", "456")
    if (result !== path) {}
}
