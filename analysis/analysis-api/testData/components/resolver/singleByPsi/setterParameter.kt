class A {
    var id: Int = 0
        set(id) {
            field = <expr>id</expr>
        }
}

// IGNORE_LOOKUP_LOCALLY
// ISSUE: KT-88734
