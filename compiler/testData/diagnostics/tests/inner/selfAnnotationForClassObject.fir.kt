// ISSUE: KT-63063

class Test {
    @ClassObjectAnnotation
    @NestedAnnotation
    companion object {
        annotation class ClassObjectAnnotation
    }

    annotation class NestedAnnotation
}
