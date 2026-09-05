class A {
    var id: Int = 0
        set(id) {
            field = <expr>id</expr>
        }
}

// ISSUE: KT-88734
