annotation class Annotation2() <!ANNOTATION_CLASS_WITH_BODY!>{
    public val s: String = ""
}<!>

annotation class Annotation3() <!ANNOTATION_CLASS_WITH_BODY!>{
    public fun foo() {}
}<!>

annotation class Annotation4() <!ANNOTATION_CLASS_WITH_BODY!>{
    class Foo() {}
}<!>

annotation class Annotation5() <!ANNOTATION_CLASS_WITH_BODY!>{
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>companion<!> object {}
}<!>

annotation class Annotation6() <!ANNOTATION_CLASS_WITH_BODY!>{
    init {}
}<!>

annotation class Annotation1() <!ANNOTATION_CLASS_WITH_BODY!>{}<!>

annotation class Annotation7(val name: String) <!ANNOTATION_CLASS_WITH_BODY!>{}<!>

annotation class Annotation8(<!VAR_ANNOTATION_PARAMETER!>var<!> name: String = "") <!ANNOTATION_CLASS_WITH_BODY!>{}<!>

annotation class Annotation9(val name: String)

annotation class Annotation10
