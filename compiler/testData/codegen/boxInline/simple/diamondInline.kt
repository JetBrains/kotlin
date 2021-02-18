
// FILE: m.kt

var result: String = ""

fun box(): String {
    am()
    if (result != "am;b;d;c;d;k;ak;b;d;") return "FAIL: $result"
    return "OK"
}

// FILE: k.kt

inline fun k() {
    result += "k;"
    ak()
}

// FILE: a.kt

inline fun am() {
    result += "am;"
    b()
    c()
}

inline fun ak() {
    result += "ak;"
    b()
}



// FILE: b.kt

inline fun b() {
    result += "b;"
    d()
}

// FILE: c.kt

inline fun c() {
    result += "c;"
    d()
    k()
}

// FILE: d.kt

inline fun d() {
    result += "d;"
}