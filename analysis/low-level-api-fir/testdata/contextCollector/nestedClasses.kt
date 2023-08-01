package test

class Foo<F> {
    class Bar<B> {
        fun test(b: B) {
            <expr>consume(b)</expr>
        }
    }
}

fun consume(obj: Any) {}