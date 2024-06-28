open class ClassWithType<T, R>(i: T, f: R)

class TypedChild : ClassWithType<Int, > {
    constructor() : <expr>super(1, "")</expr>
}