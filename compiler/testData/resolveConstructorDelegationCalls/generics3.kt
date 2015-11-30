open class B<R1, R2>(x: R1, y: R2)

class A<T1, T2> : B<T1, Int> {
    <caret>constructor(x: T1, y: Int): super(x, y) {}
}
