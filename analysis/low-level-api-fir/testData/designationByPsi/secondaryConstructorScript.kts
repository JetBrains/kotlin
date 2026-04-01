class WithConstructor(val x: Int) {
    <caret>constructor(x: Int, y: Int) : this(x)
}
