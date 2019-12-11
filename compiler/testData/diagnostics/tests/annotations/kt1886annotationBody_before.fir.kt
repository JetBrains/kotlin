// !LANGUAGE: -NestedClassesInAnnotations

annotation class Annotation2() {
    public val s: String = ""
}

annotation class Annotation3() {
    public fun foo() {}
}

annotation class Annotation4() {
    class Foo() {}
}

annotation class Annotation5() {
    companion object {}
}

annotation class Annotation6() {
    init {}
}

annotation class Annotation1() {}

annotation class Annotation7(val name: String) {}

annotation class Annotation8(var name: String = "") {}

annotation class Annotation9(val name: String)

annotation class Annotation10
