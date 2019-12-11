// !LANGUAGE: -InlineProperties
var value: Int = 0

inline var z: Int
    get() = ++value
    set(p: Int) { value = p }


var z2: Int
    inline get() = ++value
    inline set(p: Int) { value = p }
