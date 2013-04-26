package testing

open class Base {
    open var <caret>test = 12
}

open class SubBase: Base() {
    override var test = 12
}

class SubSubBase: SubBase() {
    override var test = 12
}


// REF: (testing.SubBase).test
// REF: (testing.SubSubBase).test