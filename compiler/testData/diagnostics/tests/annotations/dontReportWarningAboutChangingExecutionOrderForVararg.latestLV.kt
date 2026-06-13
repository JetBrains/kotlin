// RUN_PIPELINE_TILL: FRONTEND
// LATEST_LV_DIFFERENCE
@Retention(AnnotationRetention.RUNTIME)
annotation class Anno(vararg val x: String, val y: String)

@Anno(x = <!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>[<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNRESOLVED_REFERENCE!>["a", "b"]<!>, <!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNRESOLVED_REFERENCE!>["a", "b"]<!>]<!>, y = "a")
fun foo1() {}

@Anno(x = <!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>[<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNRESOLVED_REFERENCE!>[<!UNRESOLVED_REFERENCE!>["a"]<!>]<!>]<!>, y = "b")
fun foo11() {}

@Anno(x = ["a", "b"], y = "a")
fun foo2() {}

@Anno(x = <!ARGUMENT_TYPE_MISMATCH!>arrayOf(arrayOf("a"), arrayOf("b"))<!>, y = "a")
fun foo3() {}

@Anno(x = arrayOf("a", "b"), y = "a")
fun foo4() {}

@Retention(AnnotationRetention.RUNTIME)
annotation class Anno1(val x: <!PROJECTION_IN_TYPE_OF_ANNOTATION_MEMBER_ERROR!>Array<in String><!>, val y: String)

@Retention(AnnotationRetention.RUNTIME)
annotation class Anno2(vararg val x: String, val y: String)

@Anno1(x = ["", Anno2(x = [""], y = "")], y = "")
fun foo5() {}

/* GENERATED_FIR_TAGS: annotationDeclaration, collectionLiteral, functionDeclaration, inProjection, outProjection,
primaryConstructor, propertyDeclaration, stringLiteral, vararg */
