open class SuperFoo {
    public fun bar(): String {
        if (this is Foo) {
            <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>superFoo<!>()
            return baz()
        }
        return baz()
    }

    public fun baz() = "OK"
}

class Foo : SuperFoo() {
    public fun superFoo() {}
}
