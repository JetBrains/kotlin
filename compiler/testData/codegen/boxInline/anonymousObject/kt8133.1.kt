import test.*

interface foo {
    fun bar(): String
}

fun box(): String {
    val baz = "OK".myLet {
        object : foo {
            override fun bar() = it
        }
    }
    return baz.bar()
}