suspend inline fun f(): Int {
    val dead = false
    if (dead) println("dead1")
    return 1
}
