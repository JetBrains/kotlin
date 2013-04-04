fun foo(x: Number, y: Int) {
    when (x) {
        x as Int -> x : Int
        y -> {}
        else -> {}
    }
    <!TYPE_MISMATCH!>x<!> : Int
}

fun bar(x: Number) {
    when (x) {
        x as Int -> x : Int
        else -> {}
    }
    x : Int
}

fun whenWithoutSubject(x: Number) {
    when {
        x as Int == 42 -> x : Int
        else -> {}
    }
    x : Int
}
