package testing

interface I<T> {
    fun <caret>f(): T {

    }
}

class A<T> : I<T>

class B<T> : I<T> {
    override fun f(): T {
    }
}

class C<T> : I<T>

interface II<T>: I<T>
interface III<T>: I<T> {
    override fun f(): T {
    }
}

class A1<T>(i: I<T>) : I<T> by i

class B1<T>(i: I<T>) : I<T> by i {
    override fun f(): T {
    }
}

class C1<T>(i: I<T>) : I<T> by i

// REF: (in testing.B).f()
// REF: (in testing.B1).f()
// REF: (in testing.III).f()

