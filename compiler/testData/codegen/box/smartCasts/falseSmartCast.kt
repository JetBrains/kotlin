// IGNORE_BACKEND_FIR: JVM_IR
open class SuperFoo {
    public fun bar(): String {
        if (this is Foo) {
            superFoo() // Smart cast
            return baz() // Cannot be cast
        }
        return baz()
    }

    public fun baz() = "OK"
}

class Foo : SuperFoo() {
    public fun superFoo() {}
}

fun box(): String = Foo().bar()