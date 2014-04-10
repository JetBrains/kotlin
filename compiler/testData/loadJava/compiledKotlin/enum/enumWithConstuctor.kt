package test

enum class En(val b: Boolean = true, val i: Int = 0) {
    E1: En()
    E2: En(true, 1)
    E3: En(i = 2)
}
