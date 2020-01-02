// !WITH_NEW_INFERENCE

fun calc(x: List<String>?, y: Int?): Int {
    x?.subList(y!! - 1, y)
    // y!! above should not provide smart cast here
    return y
}
