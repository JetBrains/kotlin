// !WITH_NEW_INFERENCE

fun calc(x: List<String>?, y: Int?): Int {
    x?.get(y!! - 1) 
    // y!! above should not provide smart cast here
    val yy: Int = <!INITIALIZER_TYPE_MISMATCH!>y<!>
    return  yy + (x?.size ?: 0)
}
