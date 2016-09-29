// IS_APPLICABLE: false

class NonReachableConstructor {
    constructor<caret>(x: String)

    constructor(x: Int)
}