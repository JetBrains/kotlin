fun foo(a: Boolean, b: Boolean): Int {
    var x: Int
    if (a) {
        x = 1
    }
    when (b) {
        true -> x = 2
        false -> x = 3
    }
    return x
}

fun bar(a: Boolean, b: Boolean): Int {
    var x: Int
    if (a) {
        x = 1
    }
    when (b) {
        false -> x = 3
    }
    return x
}