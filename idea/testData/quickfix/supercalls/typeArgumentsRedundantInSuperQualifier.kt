// "Remove type arguments" "true"
interface Foo<T1, T2> {
    fun f() {}
}

class Bar: Foo<Int, Boolean> {
    fun g() {
        super<Foo<Int, <caret>Boolean>>.f();
    }
}