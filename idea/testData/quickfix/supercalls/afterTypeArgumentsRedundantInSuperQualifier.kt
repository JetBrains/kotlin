// "Remove Type Arguments List" "true"
trait Foo<T, T> {
    fun f() {}
}

class Bar: Foo<Int, Boolean> {
    fun g() {
        super<Foo<caret>>.f();
    }
}