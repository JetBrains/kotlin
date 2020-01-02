open class MyBase protected constructor() {
    protected constructor(x: Nothing?): this()
}
typealias MyAlias = MyBase

class MyDerived1 : <!INAPPLICABLE_CANDIDATE!>MyAlias<!>()
class MyDerived1a : MyBase()

class MyDerived2 : <!INAPPLICABLE_CANDIDATE!>MyAlias<!>(null)
class MyDerived2a : MyBase(null)

class MyDerived3 : MyAlias {
    constructor(x: Nothing?) : <!INAPPLICABLE_CANDIDATE!>super<!>(x)
}