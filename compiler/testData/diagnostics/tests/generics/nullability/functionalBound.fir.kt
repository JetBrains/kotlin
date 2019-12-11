// !WITH_NEW_INFERENCE
fun <E : String?, T : ((CharSequence) -> Unit)?> foo(x: E, y: T) {
    if (x != null) {
        y(x)
    }

    if (y != null) {
        y(x)
    }

    if (x != null && y != null) {
        y(x)
    }
}