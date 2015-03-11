open class B<T> {
    constructor(x: T = null!!) {}
}

class A : B<Int> {
    <caret>constructor() {}
}
