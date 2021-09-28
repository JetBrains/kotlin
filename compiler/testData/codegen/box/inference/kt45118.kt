open class Foo(open val x: Boolean)

class Bar: Foo(false) {
    val y = "OK"
}

fun contract(x: Foo) = x

val temp = if (true) contract(Bar()) else Bar()

fun box(): String = (temp as Bar).y
