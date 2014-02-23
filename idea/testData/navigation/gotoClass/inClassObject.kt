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
// REF: (test.InClassObject.object).ClassObjectClass in object in InClassObject
// REF: (test.InClassObject.object).ClassObjectObject in object in InClassObject
// REF: (test.InClassObject.object).ClassObjectTrait in object in InClassObject
// REF: (test.InClassObject.object).ClassObjectTraitWithImpl in object in InClassObject
