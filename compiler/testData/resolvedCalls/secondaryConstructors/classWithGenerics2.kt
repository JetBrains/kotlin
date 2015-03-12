class A<T, R> {
    constructor(x: T) {}
}

val y = <caret>A<Int, String>(1)