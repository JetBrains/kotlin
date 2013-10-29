fun box(): String {
    val a = true
    val x = if (a) {
        listOf(1)
    }
    else {
        1
    }
    // The result of the if() above is implicitly cast to jet.Hashable, which failed before we started to map Hashable to j.l.Object
    return "OK"
}