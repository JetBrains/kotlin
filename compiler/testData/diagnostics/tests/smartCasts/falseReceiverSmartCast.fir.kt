// RUN_PIPELINE_TILL: BACKEND
open class SuperFoo {
    public fun bar(): String {
        if (this is Foo) {
            superFoo()
            return baz()
        }
        return baz()
    }

    public fun baz() = "OK"
}

class Foo : SuperFoo() {
    public fun superFoo() {}
}
