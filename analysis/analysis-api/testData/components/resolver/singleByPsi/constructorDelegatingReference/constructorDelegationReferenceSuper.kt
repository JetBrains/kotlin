package r

abstract class BaseClass {
    constructor(i: Int)
    constructor(s: String)
}

class Child : BaseClass {
    constructor(ci: Int) : sup<caret>er(ci)
}
