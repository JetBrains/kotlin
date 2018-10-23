// IGNORE_BACKEND: JVM_IR
open class Foo(val x: () -> String)

class Outer {
    val s = "OK"

    inner class Inner : Foo({ s })
}

fun box() = Outer().Inner().x()