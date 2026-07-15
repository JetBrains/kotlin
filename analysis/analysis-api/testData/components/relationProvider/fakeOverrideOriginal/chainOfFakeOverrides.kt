package test

interface A<T> {
    fun foo(): T
}

interface B<T> {
    fun foo(): T
}

interface C<T>: A<T>, B<T>

interface D: C<String>

// function: test/D.foo
