class A {
    inner class B {
        constructor(x: String) {}
        <caret>constructor (arg: Int): this("") {}
    }
}
