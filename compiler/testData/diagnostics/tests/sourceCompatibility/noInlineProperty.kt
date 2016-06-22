// !LANGUAGE: -InlineProperties
var value: Int = 0

<!UNSUPPORTED_FEATURE!>inline<!> var z: Int
    get() = ++value
    set(p: Int) { value = p }


var z2: Int
    <!UNSUPPORTED_FEATURE!>inline<!> get() = ++value
    <!UNSUPPORTED_FEATURE!>inline<!> set(p: Int) { value = p }
