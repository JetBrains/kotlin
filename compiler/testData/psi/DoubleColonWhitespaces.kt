// COMPILATION_ERRORS

fun tests() {
    a:: b
    a ::b
    a :: b

    a?:: b
    a ?::b
    a ?:: b
    a? ::b
    a ? :: b
    a ? ? :: b
}

fun breakLine() {
    a?
    ::b
}
