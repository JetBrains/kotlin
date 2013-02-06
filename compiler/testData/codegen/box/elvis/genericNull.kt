fun foo<T: Number?>(t: T) {
    (t ?: 42).toInt()
}

fun box(): String {
    foo<Int?>(null)
    return "OK"
}
