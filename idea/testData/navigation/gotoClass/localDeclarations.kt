fun foo() {
    class LocalClass {}

    interface LocalTrait {}

    interface LocalTraitWithImpl {
        fun foo() {}
    }

    object LocalObject() {}
}

// SEARCH_TEXT: Local
// REF: (in foo).LocalClass
// REF: (in foo).LocalObject
// REF: (in foo).LocalTrait
// REF: (in foo).LocalTraitWithImpl
