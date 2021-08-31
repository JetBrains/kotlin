// !LANGUAGE: +ArrayLiteralsInAnnotations, +AssigningArraysToVarargsInNamedFormInAnnotations, +ProhibitAssigningSingleElementsToVarargsInNamedForm

// FILE: JavaAnn.java

@interface JavaAnn {
    String[] value() default {};
    String[] path() default {};
}

// FILE: test.kt

annotation class Ann(vararg val s: String)

@Ann(s = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_ANNOTATION_ERROR!>"value"<!>)
fun test1() {}

@Ann(s = *arrayOf("value"))
fun test2() {}

@Ann(s = *["value"])
fun test3() {}

@JavaAnn(value = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_ANNOTATION_ERROR!>"value"<!>)
fun test4() {}

@JavaAnn("value", path = arrayOf("path"))
fun test5() {}

@JavaAnn("value", path = ["path"])
fun test6() {}
