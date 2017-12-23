// !WITH_NEW_INFERENCE

fun calc(x: List<String>?, y: Int?): Int {
    x?.subList(y!! - 1, <!DEBUG_INFO_SMARTCAST!>y<!>)
    // y!! above should not provide smart cast here
    return <!NI;TYPE_MISMATCH, TYPE_MISMATCH!>y<!>
}
