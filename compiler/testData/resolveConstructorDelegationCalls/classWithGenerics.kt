class A<T> {
    constructor(x: T) {}
    <caret>constructor(block: () -> T): this(block()) {}
}
