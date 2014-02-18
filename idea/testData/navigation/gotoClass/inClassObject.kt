package test

class InClassObject {
    class object {
        class ClassObjectClass {}

        trait ClassObjectTrait {}

        trait ClassObjectTraitWithImpl {
            fun foo() {}
        }

        object ClassObjectObject() {}
    }
}

// SEARCH_TEXT: ClassObject
// REF: (test.InClassObject).ClassObjectClass
// REF: (test.InClassObject).ClassObjectObject
// REF: (test.InClassObject).ClassObjectTrait
// REF: (test.InClassObject).ClassObjectTraitWithImpl