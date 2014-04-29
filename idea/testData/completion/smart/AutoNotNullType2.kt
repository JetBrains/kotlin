class Foo(val prop : String?){
    fun f(foo: Foo) {
        if (foo.prop != null){
            var a: String = <caret>
        }
    }
}

// ABSENT: { itemText:"prop" }
