package d

class T {
    fun baz() = 1
}

override fun zzz() {}

fun foo(t: T) {
    override fun T.baz() = 2

    // was "Visibility is unknown yet exception"
    t.baz()

    zzz()
}
