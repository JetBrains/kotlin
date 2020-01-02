// Nothing can be cast to Nothing
fun foo(x: String) {
    x as Nothing
}

fun gav(y: String?) {
    y as Nothing
}

// Only nullable can be cast to Nothing?
fun bar(x: String, y: String?) {
    x as Nothing?
    y as Nothing?
}