annotation class My

fun foo(arg: Int): Int {
    try {
        return 1 / (arg - arg)
    } catch (e: <!WRONG_ANNOTATION_TARGET!>@My<!> Exception) {
        return -1
    }
}
