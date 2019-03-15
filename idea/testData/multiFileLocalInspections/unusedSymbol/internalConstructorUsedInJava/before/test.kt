class WithInternalConstructor(val x: Int) {
    internal <caret>constructor() : this(42)
}