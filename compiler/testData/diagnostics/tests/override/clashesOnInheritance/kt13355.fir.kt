interface IFooAny {
    val foo: Any
}

interface IFooStr : IFooAny {
    override val foo: String
}

abstract class BaseAny(override val foo: Any): IFooAny

abstract class BaseStr : BaseAny(42), IFooStr

class C : BaseStr()