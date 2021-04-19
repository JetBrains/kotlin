private var x = object {}

fun test() {
    x = <!ASSIGNMENT_TYPE_MISMATCH!>object<!> {}
}
