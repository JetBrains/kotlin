private var x = object {}

fun test() {
    x = <!TYPE_MISMATCH!>object<!> {}
}