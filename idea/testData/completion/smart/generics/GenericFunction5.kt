class C<T> {
    default object {
        fun<T> create(t: T): C<T>{}
    }
}

fun foo(c: C<String>){}

fun f(){
    foo(<caret>)
}

// EXIST: { lookupString: "create", itemText: "C.create", tailText: "(t: String) (<root>)", typeText: "C<String>" }
