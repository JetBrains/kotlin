// For KT-2796

fun test() {
    val bVal = 1
    b<caret>.app().app() // b is not there
}

// EXIST: bVal