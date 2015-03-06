package test

class InClassObject {
    default object {
        class ClassObjectClass {}

        trait ClassObjectTrait {}

        trait ClassObjectTraitWithImpl {
            fun foo() {}
        }

        object ClassObjectObject() {}
    }
}

// SEARCH_TEXT: ClassObject
// REF: (test.InClassObject.Default).ClassObjectClass
// REF: (test.InClassObject.Default).ClassObjectObject
// REF: (test.InClassObject.Default).ClassObjectTrait
// REF: (test.InClassObject.Default).ClassObjectTraitWithImpl