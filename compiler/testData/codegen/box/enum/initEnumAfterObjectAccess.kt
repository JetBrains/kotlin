// IGNORE_BACKEND: JS

var l = ""
enum class Foo {
    F;
    init {
        l += "Foo;"
    }
    object L {
        init {
            l += "Foo.CO;"
        }
    }
}

fun box(): String {
    Foo.L
    return if (l != "Foo.CO;") "FAIL: ${l}" else "OK"
}