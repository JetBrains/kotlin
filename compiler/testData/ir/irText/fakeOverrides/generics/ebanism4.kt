// FIR_IDENTICAL

interface A {
    var foo: Int
        get() = 42
        set(value) {}
}

interface B<T> {
    val foo: Int
}

class C : A, B<String> {
}
