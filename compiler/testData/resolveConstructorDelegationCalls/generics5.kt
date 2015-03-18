open class B<X, Y : X> {
    constructor(x: X, y: Y) {}
    constructor(x: X, s: String) {}
    constructor(y: Y, i: Int) : this(y, "") {}
}

class A<T1, T2 : T1> : B<T1, T2> {
    <caret>constructor(x: T2): super(x, "") {}
}
