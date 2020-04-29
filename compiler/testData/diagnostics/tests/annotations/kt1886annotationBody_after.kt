// FIR_IDENTICAL
// !LANGUAGE: +NestedClassesInAnnotations

annotation class Annotation2() {
    <!ANNOTATION_CLASS_MEMBER!>public val s: String = ""<!>
}

annotation class Annotation3() {
    <!ANNOTATION_CLASS_MEMBER!>public fun foo() {}<!>
}

annotation class Annotation4() {
    class Foo() {}
}

annotation class Annotation5() {
    companion object {}
}

annotation class Annotation6() {
    <!ANNOTATION_CLASS_MEMBER!>init {}<!>
}

annotation class Annotation1() {}

annotation class Annotation7(val name: String) {}

annotation class Annotation8(<!VAR_ANNOTATION_PARAMETER!>var<!> name: String = "") {}

annotation class Annotation9(val name: String)

annotation class Annotation10
