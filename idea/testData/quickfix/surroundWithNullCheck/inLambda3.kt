// "Surround with null check" "true"

class Foo {
    fun foo(): Foo = Foo()
}

fun bar(f: () -> Foo) {}

fun test(x: Foo?) {
    bar {
        x<caret>.foo()
    }
}