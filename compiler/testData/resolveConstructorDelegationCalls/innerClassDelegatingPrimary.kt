class A {
    inner class B(arg: String) {
        <caret>constructor (arg: Int): this("") {}
    }
}
