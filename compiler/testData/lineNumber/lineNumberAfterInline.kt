fun normalFunction() {

}

inline fun inlineFunction() {

}

fun test1() {
    inlineFunction()
    test.lineNumber()
}

fun test2() {
    normalFunction()
    test.lineNumber()
}