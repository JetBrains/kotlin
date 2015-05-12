fun foo() {
    class LocalClass {}

    interface LocalTrait {}

    interface LocalTraitWithImpl {
        fun foo() {}
    }

    object LocalObject() {}
}

// SEARCH_TEXT: Local
// REF: LocalClass
// REF: LocalObject
// REF: LocalTrait
// REF: LocalTraitWithImpl
