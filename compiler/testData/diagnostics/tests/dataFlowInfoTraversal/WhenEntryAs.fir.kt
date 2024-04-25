// CHECK_TYPE

fun foo(x: Number, y: Int) {
    when (x) {
        x as Int -> checkSubtype<Int>(x)
        y -> {}
        else -> {}
    }
    checkSubtype<Int>(x)
}

fun bar(x: Number) {
    when (x) {
        x as Int -> checkSubtype<Int>(x)
        else -> {}
    }
    checkSubtype<Int>(x)
}

fun whenWithoutSubject(x: Number) {
    when {
        (x as Int) == 42 -> checkSubtype<Int>(x)
        else -> {}
    }
    checkSubtype<Int>(x)
}
