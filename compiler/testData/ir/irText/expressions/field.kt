// MUTE_SIGNATURE_COMPARISON_K2: JS_IR
// MUTE_SIGNATURE_COMPARISON_K2: NATIVE
// ^ KT-57818

var testSimple: Int = 0
    set(value) {
        field = value
    }

var testAugmented: Int = 0
    set(value) {
        field += value
    }
