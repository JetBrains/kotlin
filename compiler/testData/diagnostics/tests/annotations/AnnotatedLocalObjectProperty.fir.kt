annotation class My

fun foo(): Int {
    val s = object {
        @My val bar: Int = 0
    }
    return s.bar
}
