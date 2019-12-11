// !CHECK_TYPE

fun foo(x: Number, y: Int) {
    when (x) {
        is Int -> checkSubtype<Int>(x)
        y -> {}
        else -> {}
    }
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Int>(x)
}

fun bar(x: Number) {
    when (x) {
        is Int -> checkSubtype<Int>(x)
        else -> {}
    }
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Int>(x)
}

fun whenWithoutSubject(x: Number) {
    when {
        (x is Int) -> checkSubtype<Int>(x)
        else -> {}
    }
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Int>(x)
}
