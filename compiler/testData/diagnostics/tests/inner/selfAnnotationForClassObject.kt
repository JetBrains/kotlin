class Test {
    [ClassObjectAnnotation]
    [NestedAnnotation]
    default object {
        annotation class ClassObjectAnnotation
    }

    annotation class NestedAnnotation
}