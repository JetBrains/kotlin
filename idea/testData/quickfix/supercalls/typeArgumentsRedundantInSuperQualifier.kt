// "Remove type arguments" "true"
interface Foo<T, T> {
    fun f() {}
}

class Bar: Foo<Int, Boolean> {
    fun g() {
        super<Foo<Int, <caret>Boolean>>.f();
    }
}