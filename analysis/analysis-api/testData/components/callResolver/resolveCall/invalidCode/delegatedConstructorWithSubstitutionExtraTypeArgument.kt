open class ClassWithType<T>(i: T)

class TypedChild : ClassWithType<Int, String> {
    constructor() : <expr>super(1)</expr>
}