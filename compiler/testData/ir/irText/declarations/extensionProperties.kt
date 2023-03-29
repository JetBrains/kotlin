// FIR_IDENTICAL

// MUTE_SIGNATURE_COMPARISON_K2: JS_IR
// MUTE_SIGNATURE_COMPARISON_K2: NATIVE
// ^ KT-57818

val String.test1 get() = 42

var String.test2
    get() = 42
    set(value) {}

class Host {
    val String.test3 get() = 42

    var String.test4
        get() = 42
        set(value) {}
}
