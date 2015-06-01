// KT-7857: when exhaustiveness does not take previous nullability checks into account
fun foo(arg: Boolean?): Int {
    if (arg != null) {
        return when (<!DEBUG_INFO_SMARTCAST!>arg<!>) {
            true -> 1
            false -> 0
            // else or null branch should not be required here!
        }
    } 
    else {
        return -1
    }
}