// ISSUE: KT-24901

fun foo(str: String?): Int {
    when {
        str == null -> return -1
    }
    if (str.length == 123)
        return 123
    return 321
}
