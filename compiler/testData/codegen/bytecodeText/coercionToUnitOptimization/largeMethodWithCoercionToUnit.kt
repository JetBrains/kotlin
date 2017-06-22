inline fun inlineFunVoid(f: () -> Unit): Unit {
    return f()
}

inline fun coercedToUnit() {
    inlineFunVoid {
        var aa = 1
        ++aa
    }
}

inline fun dup(f: () -> Unit): Unit {
    f()
    f()
}

fun test() {
    dup { dup { dup { dup { dup {
    dup { dup {
        coercedToUnit()
    }}}}}
    }}
}

// 3 POP