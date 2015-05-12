package test

class InClassObject {
    companion object {
        class ClassObjectClass {}

        interface ClassObjectTrait {}

        interface ClassObjectTraitWithImpl {
            fun foo() {}
        }

        object ClassObjectObject() {}
    }
}

// SEARCH_TEXT: ClassObject
// REF: (test.InClassObject.Companion).ClassObjectClass
// REF: (test.InClassObject.Companion).ClassObjectObject
// REF: (test.InClassObject.Companion).ClassObjectTrait
// REF: (test.InClassObject.Companion).ClassObjectTraitWithImpl