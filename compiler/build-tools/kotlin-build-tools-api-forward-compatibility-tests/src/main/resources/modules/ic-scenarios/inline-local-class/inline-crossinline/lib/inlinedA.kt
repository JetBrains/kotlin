inline fun inlinedA(crossinline block: () -> Int): Int {
    return block()
}
