@org.jetbrains.kotlin.fir.plugin.AllOpen
class Foo {
    fun call() {}
}

class Bar : Foo() {}

Bar().call()
