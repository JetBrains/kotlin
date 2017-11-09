class C<T> {
    private val blah = object {
        fun foo(): T = TODO()
    }

    fun bar(x: T) {}
}

class CC<T> {
    fun some(a: Any = object {
        fun bar(): T = TODO()
    }) {}

    fun baz(arg: T) {}
}

interface G

class CCC<T> : G by object : G {
    fun some(a: Any = object {
        fun bar(): T = null!!
    }) {}
} {
    fun baz(arg: T) {}
}

