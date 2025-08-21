interface WithGeneric<S> {
    fun <T : S> functionWithGeneric(t: T): T

    fun prop(): S
}

fun take(w: WithGeneric<*>) {
    // this is an error on purpose!
    w.<expr>functionWithGeneric</expr>
}