open class MyBase protected constructor() {
    protected constructor(x: Nothing?): this()
}
typealias MyAlias = MyBase

class MyDerived1 : MyAlias()
class MyDerived1a : MyBase()

class MyDerived2 : MyAlias(null)
class MyDerived2a : MyBase(null)

class MyDerived3 : MyAlias {
    constructor(x: Nothing?) : super(x)
}
