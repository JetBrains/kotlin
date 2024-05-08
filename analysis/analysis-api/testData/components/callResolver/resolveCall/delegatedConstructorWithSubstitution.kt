open class ClassWithType<T>(i: T)

class TypedChild : ClassWithType<Int> {
    constructor() : <expr>super(1)</expr>
}