// FIR_IDENTICAL

// The test primarily tests reflect dumps (k1 vs new reflect), we don't need kt dumps
// SKIP_KT_DUMP

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
