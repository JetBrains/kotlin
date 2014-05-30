private var x = object {}

fun test() {
    // No error, because the type of x is normalized to Any
    x = object {}
}