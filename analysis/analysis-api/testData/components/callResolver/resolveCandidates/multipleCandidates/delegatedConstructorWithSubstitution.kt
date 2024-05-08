open class ClassWithType<T>(i: T) {
    constructor(i: T, str: String) : this(i)
}

class TypedChild : ClassWithType<Int> {
    constructor() : <expr>super(1)</expr>
}