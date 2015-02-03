class A(x: Double) {
    constructor(x: Int) {}
    constructor(x: String) {}
    <caret>constructor(): this("abc") {}
}
