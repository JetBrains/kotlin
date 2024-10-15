// RUN_PIPELINE_TILL: FRONTEND
fun Any.test() {
    val x: () -> Int = when (this) {
        is String -> { { <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>length<!>  } }
        else -> { { 1 } }
    }
    <!UNRESOLVED_REFERENCE!>length<!>
}
