// IGNORE_BACKEND: JVM_IR
open class Foo(val x: () -> String)
open class Foo2(val foo: Foo)

class Outer {
    val s = "OK"

    inner class Inner : Foo2(Foo({ s }))
}

fun box() = Outer().Inner().foo.x()
