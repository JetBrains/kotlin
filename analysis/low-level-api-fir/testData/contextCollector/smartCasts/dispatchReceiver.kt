open class Foo {
    fun foo() {
        if (this is Bar) {
            <expr>consume(this)</expr>
        }
    }
}

class Bar : Foo()

fun consume(obj: Bar) {}