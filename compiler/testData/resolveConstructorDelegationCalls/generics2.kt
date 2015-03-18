class A<T1, T2> {
    constructor(x: T1, y: T2) {}
    constructor(x: T1, y: Int) {}
    <caret>constructor(x: T1): this(x, 1) {}
}
