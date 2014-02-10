fun nullNothing(): Nothing? = null
fun nullUnit(): Unit? = null
fun nullAny(): Any? = null

fun box(): String {
    if (null != null) return "Fail 1"
    if (!(null == null)) return "Fail 2"
    if (!null.equals(null)) return "Fail 3"

    if (nullNothing() != null) return "Fail 4"

    if (nullUnit() != null) return "Fail 5"

    if (nullAny() != null) return "Fail 6"

    return "OK"
}
