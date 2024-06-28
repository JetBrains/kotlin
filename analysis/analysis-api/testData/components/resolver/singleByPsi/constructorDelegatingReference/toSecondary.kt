class A(a: Int) {
    constructor(b: String) : this(2)

    constructor() : <caret>this("2")
}


