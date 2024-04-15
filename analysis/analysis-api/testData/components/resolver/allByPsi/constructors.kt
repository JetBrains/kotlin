package r

abstract class BaseClass {
    constructor(i: Int)
    constructor(s: String)

    fun call() {

    }
}

class Child : BaseClass {
    constructor(ci: Int) : super(ci)
    constructor(cs: String) : super(cs)

    fun foo() {
        super.call()
    }
}

class ChildWithPrimary() : BaseClass(42) {
    constructor(s: String) : this()
}
