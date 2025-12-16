// FIR_IDENTICAL

// Disable K1 since it reports: MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED: Class 'C' must override public open var foo: Int defined in A because it inherits multiple interface methods of it
// IGNORE_BACKEND_K1: ANY

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
