// FIR_IDENTICAL

// MUTE_SIGNATURE_COMPARISON_K2: JS_IR
// ^ KT-57818

class Test1<T : U, U>

fun <T : U, U> test2() {}

var <T : U, U> Test1<T, U>.test3: Unit
    get() {}
    set(value) {}
