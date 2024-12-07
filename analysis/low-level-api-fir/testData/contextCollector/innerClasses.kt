package test

class Foo<F> {
    inner class Bar<B> {
        fun test(f: F, b: B) {
            <expr>consume(f)</expr>
        }
    }
}

fun consume(obj: Any) {}