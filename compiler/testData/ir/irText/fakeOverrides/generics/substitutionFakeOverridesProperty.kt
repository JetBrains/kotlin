// FIR_IDENTICAL

interface A {
    var foo: Int
        get() = 42
        set(value) {}
}

interface B<T> {
    val foo: T
}

class C : A, B<Int> {
}
