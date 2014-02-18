fun foo() {
    class LocalClass {}

    trait LocalTrait {}

    trait LocalTraitWithImpl {
        fun foo() {}
    }

    object LocalObject() {}
}

// SEARCH_TEXT: Local
// REF: LocalClass
// REF: LocalObject
// REF: LocalTrait
// REF: LocalTraitWithImpl
