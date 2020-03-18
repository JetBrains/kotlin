class U {
    operator fun contains(g: String): Boolean {
        return false
    }
}


fun foo(u: U) {
    val b = false
    val i = 10
    val x = -i
    val y = !b
    val z = -1.0
    val w = +i

    val g = "" !in u
    val f = "" !is Boolean
}