// FULL_JDK
// ISSUE: KT-48113

fun collapse(path: String) {
    val result = (path as java.lang.String).replace("123", "456")
    if (result !== path) {}
}
