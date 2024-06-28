// LANGUAGE: +MultiDollarInterpolation

// TARGET_BACKEND: JS

// IGNORE_BACKEND_K1: ANY
// ISSUE: KT-69062

const val decrement = "result--;"

// interpolation prefix length: 1, 2, 4, 8
// interpolation kinds: of simple identifier, of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line
fun box(): String {
    var result = 4 * 3 * 2
    js($"$decrement")
    js($$"$$decrement")
    js($$$$"$$$$decrement")
    js($$$$$$$$"$$$$$$$$decrement")
    js($"$`decrement`")
    js($$"$$`decrement`")
    js($$$$"$$$$`decrement`")
    js($$$$$$$$"$$$$$$$$`decrement`")
    js($"${"" + decrement}")
    js($$"$${"" + decrement}")
    js($$$$"$$$${"" + decrement}")
    js($$$$$$$$"$$$$$$$${"" + decrement}")
    js($"""$decrement""")
    js($$"""$$decrement""")
    js($$$$"""$$$$decrement""")
    js($$$$$$$$"""$$$$$$$$decrement""")
    js($"""$`decrement`""")
    js($$"""$$`decrement`""")
    js($$$$"""$$$$`decrement`""")
    js($$$$$$$$"""$$$$$$$$`decrement`""")
    js($"""${"" + decrement}""")
    js($$"""$${"" + decrement}""")
    js($$$$"""$$$${"" + decrement}""")
    js($$$$$$$$"""$$$$$$$${"" + decrement}""")
    return if (result == 0) "OK" else "NOT OK: result ($result) != 0"
}
