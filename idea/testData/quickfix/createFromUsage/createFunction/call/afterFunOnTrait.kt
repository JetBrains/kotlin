// "Create function 'foo'" "true"

trait T {
    fun foo(s: String, i: Int): Boolean
}

fun test(t: T) {
    val b: Boolean = t.foo("1", 2)
}