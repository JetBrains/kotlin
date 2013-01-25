open class Base(val bar: String)

class Foo(bar: String) : Base(bar) {
  fun something() = (bar as java.lang.String).toUpperCase()
}

fun box() = Foo("ok").something()
