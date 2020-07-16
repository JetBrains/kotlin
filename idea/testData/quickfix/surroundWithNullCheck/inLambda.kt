// "Surround with null check" "true"

class Foo {
    fun foo(): Foo = Foo()
}

fun bar(f: (x: Foo?) -> Unit) {}

fun test() {
    bar {
        it<caret>.foo()
    }
}