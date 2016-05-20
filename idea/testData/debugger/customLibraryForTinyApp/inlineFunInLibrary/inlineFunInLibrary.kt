package customLib.inlineFunInLibrary

public inline fun inlineFun(f: () -> Unit) {
    1 + 1
    inlineFunInner {
        1 + 1
    }
}

public inline fun inlineFunInner(f: () -> Unit) {
    // Breakpoint 2
    1 + 1
}
