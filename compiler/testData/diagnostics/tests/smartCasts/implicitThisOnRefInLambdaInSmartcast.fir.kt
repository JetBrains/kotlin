fun Any.test() {
    val x: () -> Int = when (this) {
        is String -> { { length  } }
        else -> { { 1 } }
    }
    <!UNRESOLVED_REFERENCE!>length<!>
}
