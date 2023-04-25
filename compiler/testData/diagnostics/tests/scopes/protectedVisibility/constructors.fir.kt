// !DIAGNOSTICS: -UNUSED_PARAMETER

open class A protected constructor(x: Int) {
    protected constructor() : this(1)
    protected constructor(x: String) : this(2)
    public constructor(x: Double) : this(3)
}

fun foo() {
    <!INVISIBLE_REFERENCE!>A<!>()
    A(1.0)
}

class B1 : A(1) {}
class B2 : A() {}
class B3 : A("") {}

class B4 : A {
    constructor() : super(1)
    constructor(x: Int) : super()
    constructor(x: Int, y: Int) : super("")
}
