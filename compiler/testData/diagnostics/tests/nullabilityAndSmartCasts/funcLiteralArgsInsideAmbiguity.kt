// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE
package d

fun bar() {
    val i: Int? = 42
    if (i != null) {
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>doSmth1<!> {
        val x = <!DEBUG_INFO_SMARTCAST!>i<!> + 1
    }
}
}

fun doSmth1(f: ()->Unit) {}
fun doSmth1(g: (Int)->Unit) {}