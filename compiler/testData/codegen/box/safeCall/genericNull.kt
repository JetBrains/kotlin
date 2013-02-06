fun foo<T : Number?>(t: T) {
    t?.toInt()
}

fun box(): String {
    foo<Int?>(null)
    return "OK"
}
